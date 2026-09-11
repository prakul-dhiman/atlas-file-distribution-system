package com.sdfds.service.impl;

import com.sdfds.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail-enabled", havingValue = "false", matchIfMissing = true)
public class ConsoleEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailServiceImpl.class);

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void sendVerificationEmail(String to, String username, String verificationToken) {
        log.info("========== [DEV EMAIL] Verification ==========");
        log.info("To: {} | Username: {} | Token: {}", to, username, verificationToken);
        log.info("==============================================");
    }

    @Override
    public void sendPasswordResetEmail(String to, String username, String resetToken) {
        log.info("========== [DEV EMAIL] Password Reset ==========");
        log.info("To: {} | Username: {} | Token: {}", to, username, resetToken);
        log.info("================================================");
    }

    @Override
    public void sendDownloadNotification(String to, String ownerName, String fileName, String ipAddress) {
        log.info("========== [DEV EMAIL] Download Notification ==========");
        log.info("To: {} | '{}' downloaded your file '{}' from IP: {}", to, ownerName, fileName, ipAddress);
        log.info("=======================================================");
    }

    @Override
    public void sendOtpEmail(String to, String otpCode, String shareName) {
        log.info("========== [DEV EMAIL] OTP Code ==========");
        log.info("To: {} | Share: '{}' | OTP: {}", to, shareName, otpCode);
        log.info("=========================================");
    }
}
