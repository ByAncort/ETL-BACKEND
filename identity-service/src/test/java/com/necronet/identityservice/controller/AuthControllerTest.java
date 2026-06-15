package com.necronet.identityservice.controller;

import com.necronet.identityservice.dto.AuthRequest;
import com.necronet.identityservice.dto.RegisterRequest;
import com.necronet.identityservice.dto.UpdateEmailRequest;
import com.necronet.identityservice.dto.UpdatePasswordRequest;
import com.necronet.identityservice.service.AuthService;
import com.necronet.identityservice.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    void registerUser_shouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setEnabled(true);

        given(authService.saveUser(any())).willReturn("User registered successfully");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void registerUser_withNullEnabled_shouldDefaultToTrue() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        given(authService.saveUser(any())).willReturn("User registered successfully");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void generateToken_shouldReturn200() throws Exception {
        AuthRequest request = new AuthRequest("testuser", "password123");
        Authentication authentication = mock(Authentication.class);

        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn("testuser");
        given(authService.generateToken("testuser")).willReturn("access-token");
        given(authService.generateRefreshToken("testuser")).willReturn("refresh-token");
        given(jwtService.extractRoles("access-token")).willReturn(Set.of("ROLE_USER"));

        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authentication successful"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void generateToken_badCredentials_shouldReturn401() throws Exception {
        AuthRequest request = new AuthRequest("testuser", "wrongpassword");

        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username/email or password"));
    }

    @Test
    void generateToken_disabledUser_shouldReturn401() throws Exception {
        AuthRequest request = new AuthRequest("testuser", "password123");

        given(authenticationManager.authenticate(any()))
                .willThrow(new DisabledException("User is disabled"));

        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User account is pending verification or deactivated"));
    }

    @Test
    void validateToken_valid_shouldReturn200() throws Exception {
        given(authService.validateToken("valid-token")).willReturn(true);

        mockMvc.perform(get("/api/v1/auth/validate")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }

    @Test
    void validateToken_invalid_shouldReturn401() throws Exception {
        given(authService.validateToken("invalid-token")).willReturn(false);

        mockMvc.perform(get("/api/v1/auth/validate")
                        .param("token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token is invalid"));
    }

    @Test
    void refreshToken_valid_shouldReturn200() throws Exception {
        String refreshToken = "valid-refresh-token";
        given(authService.extractUsernameFromToken(refreshToken)).willReturn("testuser");
        given(authService.validateRefreshToken(refreshToken, "testuser")).willReturn(true);
        given(authService.generateToken("testuser")).willReturn("new-access-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void refreshToken_invalid_shouldReturn401() throws Exception {
        String refreshToken = "invalid-refresh-token";
        given(authService.extractUsernameFromToken(refreshToken)).willReturn("testuser");
        given(authService.validateRefreshToken(refreshToken, "testuser")).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void logout_shouldReturn200() throws Exception {
        willDoNothing().given(authService).invalidateToken("some-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void enableUser_shouldReturn200() throws Exception {
        willDoNothing().given(authService).enableUser("testuser");

        mockMvc.perform(get("/api/v1/auth/enable/{username}", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User enabled successfully"));
    }

    @Test
    void disableUser_shouldReturn200() throws Exception {
        willDoNothing().given(authService).disableUser("testuser");

        mockMvc.perform(get("/api/v1/auth/disable/{username}", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User disabled successfully"));
    }

    @Test
    void updatePassword_shouldReturn200() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("testuser", "newPassword123");
        willDoNothing().given(authService).updatePassword("testuser", "newPassword123");

        mockMvc.perform(put("/api/v1/auth/update-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }

    @Test
    void updateEmail_shouldReturn200() throws Exception {
        UpdateEmailRequest request = new UpdateEmailRequest("testuser", "new@example.com");
        willDoNothing().given(authService).updateEmail("testuser", "new@example.com");

        mockMvc.perform(put("/api/v1/auth/update-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email updated successfully"));
    }
}
