package com.sdfds.service.impl;

import com.sdfds.dto.*;
import com.sdfds.entity.*;
import com.sdfds.mapper.UserMapper;
import com.sdfds.repository.*;
import com.sdfds.security.JwtTokenProvider;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.AuditLogService;
import com.sdfds.service.AuthService;
import com.sdfds.service.EmailService;
import com.sdfds.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password123",
            "1234567890",
            "admin12345",
            "welcome123",
            "letmein123",
            "password1234",
            "qwerty12345",
            "iloveyou123"
    );

    private static final long VERIFICATION_TOKEN_TTL_SECONDS = 86400;
    private static final long PASSWORD_RESET_TOKEN_TTL_SECONDS = 3600;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final RateLimitService rateLimitService;

    @Value("${jwt.access-token-ttl-ms:900000}")
    private long accessTokenTtlMs;

    @Value("${jwt.refresh-token-ttl-ms:604800000}")
    private long refreshTokenTtlMs;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info(
                "Registration attempt for username: {}, email: {}",
                registerRequest.getUsername(),
                registerRequest.getEmail()
        );

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        validatePasswordPolicy(registerRequest.getPassword());

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(RoleName.ROLE_USER)
                                .description("Standard User")
                                .build()
                ));

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .isActive(true)
                .isEmailVerified(false)
                .storageQuotaBytes(5368709120L)
                .usedStorageBytes(0L)
                .roles(Set.of(userRole))
                .build();

        User savedUser = userRepository.save(user);

        log.info(
                "User registered successfully with ID: {}",
                savedUser.getId()
        );

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .user(savedUser)
                        .token(UUID.randomUUID().toString())
                        .expiryDate(
                                Instant.now()
                                        .plusSeconds(VERIFICATION_TOKEN_TTL_SECONDS)
                        )
                        .isUsed(false)
                        .build();

        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(
                savedUser.getEmail(),
                savedUser.getUsername(),
                verificationToken.getToken()
        );

        auditLogService.log(
                "REGISTER",
                "User registered: " + savedUser.getUsername(),
                savedUser
        );

        String accessToken =
                tokenProvider.generateAccessTokenFromUser(savedUser);

        RefreshToken refreshToken = createRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(accessTokenTtlMs)
                .user(userMapper.toDto(savedUser))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        log.info(
                "Login attempt for user: {}",
                loginRequest.getUsernameOrEmail()
        );

        if (!rateLimitService.isAllowed(
                loginRequest.getUsernameOrEmail(),
                5,
                60000L
        )) {
            throw new SecurityException(
                    "Too many login attempts. Please try again later."
            );
        }

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getUsernameOrEmail(),
                                loginRequest.getPassword()
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        UserPrincipal userPrincipal =
                (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(
                        () -> new IllegalArgumentException("User not found")
                );

        String accessToken =
                tokenProvider.generateAccessToken(authentication);

        RefreshToken refreshToken = createRefreshToken(user);

        auditLogService.log(
                "LOGIN",
                "User logged in: " + user.getUsername(),
                user
        );

        log.info(
                "User logged in successfully with ID: {}",
                user.getId()
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(accessTokenTtlMs)
                .user(userMapper.toDto(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(
            RefreshTokenRequest refreshTokenRequest
    ) {
        String tokenStr = refreshTokenRequest.getRefreshToken();

        log.info("Refreshing token");

        RefreshToken token = refreshTokenRepository
                .findByToken(tokenStr)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Invalid refresh token"
                        )
                );

        if (token.getIsRevoked()) {
            log.warn(
                    "Revoked refresh token presented for user ID: {}",
                    token.getUser().getId()
            );

            throw new IllegalArgumentException(
                    "Refresh token has been revoked"
            );
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);

            throw new IllegalArgumentException(
                    "Refresh token has expired"
            );
        }

        token.setIsRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();

        String newAccessToken =
                tokenProvider.generateAccessTokenFromUser(user);

        RefreshToken newRefreshToken =
                createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(accessTokenTtlMs)
                .user(userMapper.toDto(user))
                .build();
    }

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        log.info(
                "Password reset requested for email: {}",
                request.getEmail()
        );

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElse(null);

        if (user != null) {
            passwordResetTokenRepository.deleteByUser(user);

            PasswordResetToken resetToken =
                    PasswordResetToken.builder()
                            .user(user)
                            .token(UUID.randomUUID().toString())
                            .expiryDate(
                                    Instant.now()
                                            .plusSeconds(
                                                    PASSWORD_RESET_TOKEN_TTL_SECONDS
                                            )
                            )
                            .isUsed(false)
                            .build();

            passwordResetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getUsername(),
                    resetToken.getToken()
            );

            auditLogService.log(
                    "PASSWORD_RESET_REQUESTED",
                    "Password reset requested for user: "
                            + user.getUsername(),
                    user
            );

            log.info(
                    "Password reset token generated for user ID: {}",
                    user.getId()
            );
        }
    }

    @Override
    @Transactional
    public void confirmPasswordReset(
            PasswordResetConfirmRequest request
    ) {
        validatePasswordPolicy(request.getNewPassword());

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(request.getToken())
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Invalid or expired password reset token"
                                )
                        );

        if (resetToken.getIsUsed()
                || resetToken.getExpiryDate().isBefore(Instant.now())) {

            throw new IllegalArgumentException(
                    "Password reset token has expired or already been used"
            );
        }

        User user = resetToken.getUser();

        user.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);

        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.deleteByUser(user);

        auditLogService.log(
                "PASSWORD_RESET_CONFIRMED",
                "Password reset confirmed for user: "
                        + user.getUsername(),
                user
        );

        log.info(
                "Password reset completed successfully for user ID: {}",
                user.getId()
        );
    }

    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        if (refreshTokenStr != null) {
            refreshTokenRepository
                    .findByToken(refreshTokenStr)
                    .ifPresent(token -> {

                        token.setIsRevoked(true);
                        refreshTokenRepository.save(token);

                        auditLogService.log(
                                "LOGOUT",
                                "User logged out: "
                                        + token.getUser().getUsername(),
                                token.getUser()
                        );

                        log.info(
                                "Refresh token revoked during logout "
                                        + "for user ID: {}",
                                token.getUser().getId()
                        );
                    });
        }
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository
                        .findByToken(token)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Invalid verification token"
                                )
                        );

        if (Boolean.TRUE.equals(verificationToken.getIsUsed())
                || verificationToken.getExpiryDate()
                        .isBefore(Instant.now())) {

            throw new IllegalArgumentException(
                    "Verification token has expired or already been used"
            );
        }

        User user = verificationToken.getUser();

        user.setIsEmailVerified(true);
        userRepository.save(user);

        verificationToken.setIsUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        auditLogService.log(
                "EMAIL_VERIFIED",
                "User email verified: " + user.getEmail(),
                user
        );

        log.info(
                "Email verified for user ID: {}",
                user.getId()
        );
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElse(null);

        if (user == null
                || Boolean.TRUE.equals(user.getIsEmailVerified())) {

            log.info(
                    "Verification email resend skipped for "
                            + "non-existent or already verified email"
            );

            return;
        }

        emailVerificationTokenRepository.deleteByUser(user);

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .user(user)
                        .token(UUID.randomUUID().toString())
                        .expiryDate(
                                Instant.now()
                                        .plusSeconds(
                                                VERIFICATION_TOKEN_TTL_SECONDS
                                        )
                        )
                        .isUsed(false)
                        .build();

        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getUsername(),
                verificationToken.getToken()
        );

        auditLogService.log(
                "VERIFICATION_RESENT",
                "Verification email resent for: "
                        + user.getEmail(),
                user
        );

        log.info(
                "Verification email resent for user ID: {}",
                user.getId()
        );
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token =
                RefreshToken.builder()
                        .user(user)
                        .token(UUID.randomUUID().toString())
                        .expiryDate(
                                Instant.now()
                                        .plusMillis(refreshTokenTtlMs)
                        )
                        .isRevoked(false)
                        .build();

        return refreshTokenRepository.save(token);
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 10) {
            throw new IllegalArgumentException(
                    "Password must be at least 10 characters long"
            );
        }

        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Password is too common and easily guessable"
            );
        }
    }
}

