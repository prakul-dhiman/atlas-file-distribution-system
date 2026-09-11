package com.sdfds.service.impl;

import com.sdfds.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Real SMTP-based email sender. Active only when app.mail-enabled=true.
 * Sends actual password-reset, email-verification, and download-notification emails.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail-enabled", havingValue = "true")
public class SmtpEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Override
    public void sendVerificationEmail(String to, String username, String verificationToken) {
        String link = frontendUrl + "/verify-email?token=" + verificationToken;
        String subject = "Verify your email — SDFDS";
        String body = "Hello " + username + ",\n\n"
                + "Please verify your email address by clicking the link below:\n\n"
                + link + "\n\n"
                + "This link expires in 24 hours. If you did not request this, you can ignore this email.\n\n"
                + "Thanks,\nThe SDFDS Team";
        send(to, subject, body);
    }

    @Override
    public void sendPasswordResetEmail(String to, String username, String resetToken) {
        String link = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "Reset your password — SDFDS";
        String body = "Hello " + username + ",\n\n"
                + "We received a request to reset your password. Click the link below to set a new password:\n\n"
                + link + "\n\n"
                + "This link expires in 1 hour. If you did not request this, please ignore this email.\n\n"
                + "Thanks,\nThe SDFDS Team";
        send(to, subject, body);
    }

    @Override
    public void sendDownloadNotification(String to, String ownerName, String fileName, String ipAddress) {
        String subject = "Your file was downloaded — SDFDS";
        String body = "Hi " + ownerName + ",\n\n"
                + "Your file '" + fileName + "' was downloaded from IP: " + ipAddress + ".\n\n"
                + "Thanks,\nThe SDFDS Team";
        send(to, subject, body);
    }

    @Override
    public void sendOtpEmail(String to, String otpCode, String shareName) {
        String subject = "Your access code for \"" + shareName + "\" — SDFDS";
        String body = "Hello,\n\n"
                + "You requested access to the shared file/folder: \"" + shareName + "\".\n\n"
                + "Your one-time access code is:\n\n"
                + "    " + otpCode + "\n\n"
                + "This code expires in 10 minutes and can only be used once.\n\n"
                + "If you did not request this code, you can safely ignore this email.\n\n"
                + "Thanks,\nThe SDFDS Security Team";
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        // If SMTP credentials are not configured (empty username), fall back to
        // logging the email to console so dev flows (forgot password, verify email)
        // still work without a real mail server.
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("SMTP not configured (spring.mail.username is empty). Falling back to console log for email to: {}", to);
            log.info("========== [DEV EMAIL] {} ==========", subject);
            log.info("To: {} | Subject: {}", to, subject);
            log.info("Body: {}", body);
            log.info("=====================================");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} — subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} — subject: {}", to, subject, e);
        }
    }
}
