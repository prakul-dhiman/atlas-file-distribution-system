# ADR-0002: Phase 1 Authentication & User Management Design

- **Status**: Accepted
- **Date**: 2026-07-27
- **Context**: The system requires secure user authentication, role-based authorization (USER, ADMIN), password policy enforcement, and JWT token issuance and rotation.

## Design Rationale
Stateless authentication is implemented using signed JWT access tokens (15-minute TTL) alongside database-persisted refresh tokens (7-day TTL). When a refresh token is presented, it is validated, revoked/rotated, and a fresh access/refresh pair is issued. Passwords are hashed using BCrypt (strength 12) and validated against both length (min 10 chars) and a common-password blocklist per Section 3 policy. Role-based security (ROLE_USER, ROLE_ADMIN) is enforced via Spring Security `@PreAuthorize` annotations and custom JWT authentication filters.

## Files Touched
- `backend/docs/decisions/ADR-0002-Phase-1-Auth-And-Users.md`
- `backend/src/main/java/com/sdfds/entity/User.java`
- `backend/src/main/java/com/sdfds/entity/Role.java`
- `backend/src/main/java/com/sdfds/entity/Permission.java`
- `backend/src/main/java/com/sdfds/entity/RefreshToken.java`
- `backend/src/main/java/com/sdfds/entity/PasswordResetToken.java`
- `backend/src/main/java/com/sdfds/entity/RoleName.java`
- `backend/src/main/java/com/sdfds/repository/UserRepository.java`
- `backend/src/main/java/com/sdfds/repository/RoleRepository.java`
- `backend/src/main/java/com/sdfds/repository/RefreshTokenRepository.java`
- `backend/src/main/java/com/sdfds/repository/PasswordResetTokenRepository.java`
- `backend/src/main/java/com/sdfds/security/JwtTokenProvider.java`
- `backend/src/main/java/com/sdfds/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/sdfds/security/UserPrincipal.java`
- `backend/src/main/java/com/sdfds/security/UserDetailsServiceImpl.java`
- `backend/src/main/java/com/sdfds/dto/RegisterRequest.java`
- `backend/src/main/java/com/sdfds/dto/LoginRequest.java`
- `backend/src/main/java/com/sdfds/dto/RefreshTokenRequest.java`
- `backend/src/main/java/com/sdfds/dto/PasswordResetRequest.java`
- `backend/src/main/java/com/sdfds/dto/PasswordResetConfirmRequest.java`
- `backend/src/main/java/com/sdfds/dto/AuthResponse.java`
- `backend/src/main/java/com/sdfds/dto/UserDto.java`
- `backend/src/main/java/com/sdfds/service/AuthService.java`
- `backend/src/main/java/com/sdfds/service/impl/AuthServiceImpl.java`
- `backend/src/main/java/com/sdfds/service/UserService.java`
- `backend/src/main/java/com/sdfds/service/impl/UserServiceImpl.java`
- `backend/src/main/java/com/sdfds/controller/AuthController.java`
- `backend/src/main/java/com/sdfds/controller/UserController.java`
- `backend/src/main/java/com/sdfds/config/SecurityConfig.java`
- `backend/src/test/java/com/sdfds/service/AuthServiceTest.java`
- `backend/src/test/java/com/sdfds/controller/AuthControllerTest.java`

## Acceptance Criteria Satisfied
1. User registration with BCrypt password hashing and password policy validation (min 10 chars, common password rejection).
2. User authentication (Login) issuing signed JWT access token (15-min TTL) and refresh token (7-day TTL).
3. Access token validation via `JwtAuthenticationFilter` on protected endpoints (e.g. `GET /api/v1/users/me`).
4. Refresh token endpoint (`POST /api/v1/auth/refresh`) supporting single-use token rotation.
5. Password reset workflow via token request and confirmation.
