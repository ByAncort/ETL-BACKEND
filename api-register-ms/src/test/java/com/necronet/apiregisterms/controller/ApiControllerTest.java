package com.necronet.apiregisterms.controller;

import com.necronet.apiregisterms.dto.ApiResponse;
import com.necronet.apiregisterms.service.ApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tema 3 — Validacion de Datos.
 * Verifica que un payload invalido sea rechazado con 400 Bad Request
 * (via @Valid + @NotBlank) en lugar de provocar un 500.
 */
@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiService apiService;

    @Test
    @DisplayName("Payload valido devuelve 200")
    void registerApi_valid_returns200() throws Exception {
        when(apiService.registerApi(any())).thenReturn(null);
        when(apiService.toResponse(any()))
                .thenReturn(ApiResponse.builder().id(1L).url("http://api.test").build());

        mockMvc.perform(post("/api-registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"GET\",\"url\":\"http://api.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Falta url -> 400 (no llega al service)")
    void registerApi_missingUrl_returns400() throws Exception {
        mockMvc.perform(post("/api-registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"GET\"}"))
                .andExpect(status().isBadRequest());

        verify(apiService, never()).registerApi(any());
    }

    @Test
    @DisplayName("Falta method -> 400")
    void registerApi_missingMethod_returns400() throws Exception {
        mockMvc.perform(post("/api-registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://api.test\"}"))
                .andExpect(status().isBadRequest());

        verify(apiService, never()).registerApi(any());
    }

    @Test
    @DisplayName("url en blanco -> 400")
    void registerApi_blankUrl_returns400() throws Exception {
        mockMvc.perform(post("/api-registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"GET\",\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
