package com.necronet.integrationms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.necronet.integrationms.dto.LogRequest;
import com.necronet.integrationms.entity.ExecutionLog;
import com.necronet.integrationms.entity.LogLevel;
import com.necronet.integrationms.repository.ExecutionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExecutionLogRepository logRepository;

    @Test
    void saveLog_shouldReturn200() throws Exception {
        LogRequest request = new LogRequest();
        request.setParentId("parent-123");
        request.setClassName("TestService");
        request.setMethodName("doSomething");
        request.setLogLevel("INFO");
        request.setMessage("test message");
        request.setDetail("detail");
        request.setDurationMs(100L);
        request.setIntegrationId("42");

        given(logRepository.save(any(ExecutionLog.class))).willAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/integrations/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        then(logRepository).should().save(any(ExecutionLog.class));
    }

    @Test
    void getLogs_byIntegrationId_shouldReturn200() throws Exception {
        ExecutionLog log = ExecutionLog.builder()
                .id("log-1")
                .integrationId("42")
                .executionId("exec-1")
                .serviceName("integration-ms")
                .className("TestService")
                .methodName("test")
                .logLevel(LogLevel.INFO)
                .message("test")
                .timestamp(LocalDateTime.now())
                .build();

        given(logRepository.findByIntegrationIdOrderByTimestampDesc("42")).willReturn(List.of(log));

        mockMvc.perform(get("/api/integrations/logs")
                .param("integrationId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value("log-1"))
                .andExpect(jsonPath("$[0].integrationId").value("42"));
    }

    @Test
    void getLogs_byExecutionId_shouldReturn200() throws Exception {
        ExecutionLog log = ExecutionLog.builder()
                .id("log-2")
                .executionId("exec-99")
                .logLevel(LogLevel.INFO)
                .message("test")
                .timestamp(LocalDateTime.now())
                .build();

        given(logRepository.findByExecutionIdOrderByTimestampAsc("exec-99")).willReturn(List.of(log));

        mockMvc.perform(get("/api/integrations/logs")
                .param("executionId", "exec-99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value("log-2"));
    }

    @Test
    void getLogs_byLogLevel_shouldReturn200() throws Exception {
        ExecutionLog log = ExecutionLog.builder()
                .id("log-3")
                .logLevel(LogLevel.ERROR)
                .message("error occurred")
                .timestamp(LocalDateTime.now())
                .build();

        given(logRepository.findByLogLevelOrderByTimestampDesc(LogLevel.ERROR)).willReturn(List.of(log));

        mockMvc.perform(get("/api/integrations/logs")
                .param("logLevel", "ERROR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value("log-3"))
                .andExpect(jsonPath("$[0].logLevel").value("ERROR"));
    }

    @Test
    void getLogs_all_shouldReturn200() throws Exception {
        ExecutionLog log1 = ExecutionLog.builder()
                .id("log-1")
                .logLevel(LogLevel.INFO)
                .message("msg1")
                .timestamp(LocalDateTime.now())
                .build();

        ExecutionLog log2 = ExecutionLog.builder()
                .id("log-2")
                .logLevel(LogLevel.WARN)
                .message("msg2")
                .timestamp(LocalDateTime.now())
                .build();

        given(logRepository.findAll()).willReturn(List.of(log1, log2));

        mockMvc.perform(get("/api/integrations/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));
    }

    @Test
    void getLogById_whenFound_shouldReturn200() throws Exception {
        ExecutionLog log = ExecutionLog.builder()
                .id("log-found")
                .executionId("exec-1")
                .logLevel(LogLevel.INFO)
                .message("found log")
                .timestamp(LocalDateTime.now())
                .build();

        given(logRepository.findById("log-found")).willReturn(Optional.of(log));

        mockMvc.perform(get("/api/integrations/logs/log-found"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("log-found"))
                .andExpect(jsonPath("$.message").value("found log"));
    }

    @Test
    void getLogById_whenNotFound_shouldReturn404() throws Exception {
        given(logRepository.findById("nonexistent")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/integrations/logs/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
