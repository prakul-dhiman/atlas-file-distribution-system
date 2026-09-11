package com.sdfds.service;

import com.sdfds.dto.AuthResponse;
import com.sdfds.dto.LoginRequest;
import com.sdfds.dto.RegisterRequest;
import com.sdfds.entity.*;
import com.sdfds.mapper.UserMapper;
import com.sdfds.repository.*;
import com.sdfds.security.JwtTokenProvider;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private RateLimitService rateLimitService;

    @Spy
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@atlas.io")
                .password("SecurePass123!")
                .firstName("Atlas")
                .lastName("Tester")
                .build();

        userRole = Role.builder().id(1L).name(RoleName.ROLE_USER).build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@atlas.io")
                .passwordHash("hashedPass")
                .roles(Set.of(userRole))
                .build();
    }

    @Test
    @DisplayName("Register - Success")
    void register_Success() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateAccessTokenFromUser(any(User.class))).thenReturn("access_token_123");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access_token_123", response.getAccessToken());
        assertEquals("testuser", response.getUser().getUsername());
        verify(userRepository).save(any(User.class));
        // Email verification token generated + sent + audit logged
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationEmail(eq("test@atlas.io"), eq("testuser"), anyString());
        verify(auditLogService).log(eq("REGISTER"), anyString(), any(User.class));
    }

    @Test
    @DisplayName("Register - Fail on duplicate username")
    void register_DuplicateUsername() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Username is already taken", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register - Fail on common password")
    void register_CommonPassword() {
        registerRequest.setPassword("password123");
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertTrue(ex.getMessage().contains("Password is too common"));
    }

    @Test
    @DisplayName("Login - Success with audit log")
    void login_Success() {
        UserPrincipal principal = new UserPrincipal(testUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("SecurePass123!")
                .build();

        when(rateLimitService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access_token_123");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access_token_123", response.getAccessToken());
        verify(auditLogService).log(eq("LOGIN"), anyString(), eq(testUser));
    }

    @Test
    @DisplayName("Login - Rate limited after too many attempts")
    void login_RateLimited() {
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("SecurePass123!")
                .build();

        when(rateLimitService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(false);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> authService.login(loginRequest));

        assertEquals("Too many login attempts. Please try again later.", ex.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Verify email - Success")
    void verifyEmail_Success() {
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .id(1L)
                .user(testUser)
                .token("verify-token-123")
                .expiryDate(Instant.now().plusSeconds(3600))
                .isUsed(false)
                .build();

        when(emailVerificationTokenRepository.findByToken("verify-token-123")).thenReturn(Optional.of(verificationToken));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.verifyEmail("verify-token-123");

        assertTrue(testUser.getIsEmailVerified());
        assertTrue(verificationToken.getIsUsed());
        verify(auditLogService).log(eq("EMAIL_VERIFIED"), anyString(), eq(testUser));
    }

    @Test
    @DisplayName("Verify email - Expired token rejected")
    void verifyEmail_ExpiredToken() {
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .id(1L)
                .user(testUser)
                .token("verify-token-expired")
                .expiryDate(Instant.now().minusSeconds(3600))
                .isUsed(false)
                .build();

        when(emailVerificationTokenRepository.findByToken("verify-token-expired")).thenReturn(Optional.of(verificationToken));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.verifyEmail("verify-token-expired"));

        assertEquals("Verification token has expired or already been used", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}