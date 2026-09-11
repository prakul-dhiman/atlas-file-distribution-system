package com.sdfds.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdfds.dto.AuthResponse;
import com.sdfds.dto.LoginRequest;
import com.sdfds.dto.RegisterRequest;
import com.sdfds.dto.UserDto;
import com.sdfds.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/register - Success")
    void register_ShouldReturn201Created() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("atlasuser")
                .email("user@atlas.io")
                .password("ComplexPassword123!")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock_access_token")
                .refreshToken("mock_refresh_token")
                .tokenType("Bearer")
                .expiresInMs(900000L)
                .user(UserDto.builder().id(1L).username("atlasuser").email("user@atlas.io").roles(Set.of("ROLE_USER")).build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"))
                .andExpect(jsonPath("$.data.user.username").value("atlasuser"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Success")
    void login_ShouldReturn200Ok() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("atlasuser")
                .password("ComplexPassword123!")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock_access_token")
                .refreshToken("mock_refresh_token")
                .tokenType("Bearer")
                .expiresInMs(900000L)
                .user(UserDto.builder().id(1L).username("atlasuser").email("user@atlas.io").roles(Set.of("ROLE_USER")).build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }
}
