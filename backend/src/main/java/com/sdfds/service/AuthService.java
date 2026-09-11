package com.sdfds.service;

import com.sdfds.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    void requestPasswordReset(PasswordResetRequest request);

    void confirmPasswordReset(PasswordResetConfirmRequest request);

    void logout(String refreshToken);

    void verifyEmail(String token);

    void resendVerificationEmail(String email);
}