package com.sdfds.service.impl;

import com.sdfds.entity.ShareOtpCode;
import com.sdfds.entity.SharedLink;
import com.sdfds.repository.ShareOtpCodeRepository;
import com.sdfds.repository.SharedLinkRepository;
import com.sdfds.service.EmailService;
import com.sdfds.service.ShareOtpService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareOtpServiceImpl implements ShareOtpService {

    private static final Logger log = LoggerFactory.getLogger(ShareOtpServiceImpl.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;
    private static final int RATE_LIMIT_MAX_REQUESTS = 3;

    private final ShareOtpCodeRepository otpCodeRepository;
    private final SharedLinkRepository sharedLinkRepository;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void sendOtp(String token, String email, String ipAddress) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Share link not found"));

        // Rate limiting: max 3 OTP requests per IP+email per 15 min
        String rateLimitKey = "otp:rate:" + ipAddress + ":" + email.toLowerCase();
        String rateLimitCount = redisTemplate.opsForValue().get(rateLimitKey);
        int currentCount = rateLimitCount != null ? Integer.parseInt(rateLimitCount) : 0;
        if (currentCount >= RATE_LIMIT_MAX_REQUESTS) {
            throw new SecurityException("Too many OTP requests. Please wait " + RATE_LIMIT_WINDOW_MINUTES + " minutes before trying again.");
        }

        // Generate 6-digit OTP
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(1_000_000));

        // Save OTP record
        ShareOtpCode otpCode = ShareOtpCode.builder()
                .sharedLink(link)
                .email(email.toLowerCase().trim())
                .otpCode(otp)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(OTP_EXPIRY_MINUTES)))
                .build();
        otpCodeRepository.save(otpCode);

        // Increment rate limit counter
        if (rateLimitCount == null) {
            redisTemplate.opsForValue().set(rateLimitKey, "1", Duration.ofMinutes(RATE_LIMIT_WINDOW_MINUTES));
        } else {
            redisTemplate.opsForValue().increment(rateLimitKey);
        }

        // Send OTP email
        String shareName = link.getFile() != null ? link.getFile().getName()
                : link.getFolder() != null ? link.getFolder().getName() : "Shared Item";
        try {
            emailService.sendOtpEmail(email, otp, shareName);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
        }
        log.info("OTP sent to {} for share token: {}", email, token);
    }

    @Override
    @Transactional
    public String verifyOtp(String token, String email, String otp) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Share link not found"));

        String normalizedEmail = email.toLowerCase().trim();

        // Brute-force key per share+email
        String bruteKey = "otp:brute:" + token + ":" + normalizedEmail;
        String failCount = redisTemplate.opsForValue().get(bruteKey);
        int failures = failCount != null ? Integer.parseInt(failCount) : 0;
        if (failures >= MAX_ATTEMPTS) {
            throw new SecurityException("Too many failed attempts. Please request a new OTP.");
        }

        ShareOtpCode otpCode = otpCodeRepository
                .findTopBySharedLinkAndEmailAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        link, normalizedEmail, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("No valid OTP found. Please request a new code."));

        if (!otp.equals(otpCode.getOtpCode())) {
            // Increment failure counter
            otpCode.setAttemptCount(otpCode.getAttemptCount() + 1);
            otpCodeRepository.save(otpCode);
            if (failCount == null) {
                redisTemplate.opsForValue().set(bruteKey, "1", Duration.ofMinutes(30));
            } else {
                redisTemplate.opsForValue().increment(bruteKey);
            }
            throw new IllegalArgumentException("Invalid OTP code. " + (MAX_ATTEMPTS - failures - 1) + " attempt(s) remaining.");
        }

        // Mark OTP as used
        otpCode.setIsUsed(true);
        otpCodeRepository.save(otpCode);

        // Clear brute-force counter on success
        redisTemplate.delete(bruteKey);

        // Issue a short-lived session token stored in Redis (30 min)
        String sessionToken = UUID.randomUUID().toString().replace("-", "");
        String sessionKey = "otp:session:" + token + ":" + normalizedEmail;
        redisTemplate.opsForValue().set(sessionKey, sessionToken, Duration.ofMinutes(30));

        log.info("OTP verified successfully for {} on share token: {}", normalizedEmail, token);
        return sessionToken;
    }
}
