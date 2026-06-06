package com.necronet.integrationms.controller;

import com.necronet.integrationms.entity.ExecutionLog;
import com.necronet.integrationms.entity.LogLevel;
import com.necronet.integrationms.repository.ExecutionLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tema 4 — Procesos Async ETL ("proceso registrado").
 * Verifica el endpoint /api/integrations/logs que usan los demas servicios
 * (p.ej. MS-SAVE-DATA) para notificar el avance/fin del proceso ETL.
 */
@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecutionLogRepository logRepository;

    @Test
    @DisplayName("POST /api/integrations/logs persiste el log y devuelve 200")
    void saveLog_persistsAndReturns200() throws Exception {
        mockMvc.perform(post("/api/integrations/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"className\":\"EtlOrchestrator\",\"methodName\":\"run_etl\"," +
                                "\"logLevel\":\"INFO\",\"message\":\"Proceso finalizado\"," +
                                "\"integrationId\":\"42\",\"durationMs\":1500}"))
                .andExpect(status().isOk());

        verify(logRepository).save(any(ExecutionLog.class));
    }

    @Test
    @DisplayName("logLevel en minuscula se normaliza al enum correcto")
    void saveLog_normalizesLogLevel() throws Exception {
        mockMvc.perform(post("/api/integrations/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"className\":\"C\",\"methodName\":\"m\"," +
                                "\"logLevel\":\"error\",\"message\":\"fallo\"}"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<ExecutionLog> captor =
                org.mockito.ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getLogLevel())
                .isEqualTo(LogLevel.ERROR);
    }

    @Test
    @DisplayName("GET /api/integrations/logs?integrationId consulta por integracion")
    void getLogs_byIntegrationId() throws Exception {
        when(logRepository.findByIntegrationIdOrderByTimestampDesc("42"))
                .thenReturn(List.of(ExecutionLog.builder()
                        .id("1").executionId("e").serviceName("s")
                        .className("c").methodName("m")
                        .logLevel(LogLevel.INFO).message("ok")
                        .build()));

        mockMvc.perform(get("/api/integrations/logs").param("integrationId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("ok"));

        verify(logRepository).findByIntegrationIdOrderByTimestampDesc("42");
    }
}
