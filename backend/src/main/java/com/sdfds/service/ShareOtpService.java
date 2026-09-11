package com.sdfds.service;

public interface ShareOtpService {
    /** Send OTP to the given email for this share token. */
    void sendOtp(String token, String email, String ipAddress);

    /** Verify the submitted OTP, returns the session token on success. */
    String verifyOtp(String token, String email, String otp);
}
