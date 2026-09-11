package com.sdfds.service;

public interface EmailService {

    void sendVerificationEmail(String to, String username, String verificationToken);

    void sendPasswordResetEmail(String to, String username, String resetToken);

    void sendDownloadNotification(String to, String ownerName, String fileName, String ipAddress);

    void sendOtpEmail(String to, String otpCode, String shareName);
}
