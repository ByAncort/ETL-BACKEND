package com.necronet.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.necronet.identityservice.dto.AuthRequest;
import com.necronet.identityservice.service.AuthService;
import com.necronet.identityservice.service.JwtService;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tema 1 — Seguridad y Autenticacion.
 * Pruebas de la capa web (MockMvc) sobre AuthController.
 * Dependencias mockeadas; sin BD ni red. Filtros de seguridad desactivados
 * para aislar la logica del controlador.
 */
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
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Login correcto devuelve 200 y access token")
    void token_validCredentials_returns200() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("diego");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(authService.generateToken("diego")).thenReturn("access-tok");
        when(authService.generateRefreshToken("diego")).thenReturn("refresh-tok");
        when(jwtService.extractRoles("access-tok")).thenReturn(Set.of("ROLE_USER"));

        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("diego", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-tok"))
                .andExpect(jsonPath("$.message").value("Authentication successful"));
    }

    @Test
    @DisplayName("Credenciales invalidas devuelven 401")
    void token_badCredentials_returns401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("diego", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("Usuario desactivado no puede iniciar sesion (DisabledException -> 401)")
    void token_disabledUser_returns401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("disabled"));

        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("diego", "secret123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User account is pending verification or deactivated"));
    }

    @Test
    @DisplayName("Logout agrega el token a la blacklist")
    void logout_invalidatesToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).invalidateToken("abc.def.ghi");
    }

    @Test
    @DisplayName("Desactivar usuario invoca disableUser y devuelve 200")
    void disableUser_callsService() throws Exception {
        mockMvc.perform(get("/api/v1/auth/disable/diego"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User disabled successfully"));

        verify(authService).disableUser("diego");
    }

    @Test
    @DisplayName("Validar token invalido devuelve 401")
    void validate_invalidToken_returns401() throws Exception {
        when(authService.validateToken("bad")).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/validate").param("token", "bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token is invalid"));
    }
}
