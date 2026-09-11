package com.sdfds.controller;

import com.sdfds.dto.*;
import com.sdfds.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, token refresh, and password management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns access/refresh JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and issues access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Validates refresh token and issues a new token pair (token rotation)")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthResponse response = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Request password reset", description = "Generates password reset token for user email")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("If the email exists, a password reset token has been generated", "Reset link sent"));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Confirm password reset", description = "Resets user password using valid token")
    public ResponseEntity<ApiResponse<String>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", "Password reset successful"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes user refresh token")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "Logged out"));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email", description = "Confirms a user's email address using a verification token")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", "Email verified"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email", description = "Sends a new email verification link to the user")
    public ResponseEntity<ApiResponse<String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("If the email exists and is unverified, a new verification link has been sent", "Verification email sent"));
    }
}
