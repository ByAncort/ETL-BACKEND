package com.necronet.integrationms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.necronet.integrationms.dto.IntegrationRequest;
import com.necronet.integrationms.dto.IntegrationResponse;
import com.necronet.integrationms.entity.IntegrationStatus;
import com.necronet.integrationms.service.IntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IntegrationController.class)
class IntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IntegrationService integrationService;

    @Test
    void createIntegration_shouldReturn201() throws Exception {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("api-1");
        request.setApiB("api-2");
        request.setDescription("test desc");

        IntegrationResponse response = new IntegrationResponse();
        response.setId(1L);
        response.setApiA("api-1");
        response.setApiB("api-2");
        response.setDescription("test desc");
        response.setStatus(IntegrationStatus.ACTIVE);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());

        given(integrationService.createIntegration(any(IntegrationRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/integrations/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.apiA").value("api-1"))
                .andExpect(jsonPath("$.apiB").value("api-2"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAllIntegrations_shouldReturn200() throws Exception {
        IntegrationResponse r1 = new IntegrationResponse();
        r1.setId(1L);
        r1.setApiA("api-1");
        r1.setApiB("api-2");

        IntegrationResponse r2 = new IntegrationResponse();
        r2.setId(2L);
        r2.setApiA("api-3");
        r2.setApiB("api-4");

        given(integrationService.getAllIntegrations()).willReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/integrations/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getAllIntegrationsIncludingDeleted_shouldReturn200() throws Exception {
        IntegrationResponse r1 = new IntegrationResponse();
        r1.setId(1L);
        r1.setApiA("api-1");
        r1.setApiB("api-2");

        given(integrationService.getAllIntegrationsIncludingDeleted()).willReturn(List.of(r1));

        mockMvc.perform(get("/api/integrations/connections/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getIntegrationById_shouldReturn200() throws Exception {
        IntegrationResponse response = new IntegrationResponse();
        response.setId(5L);
        response.setApiA("api-a");
        response.setApiB("api-b");
        response.setStatus(IntegrationStatus.ACTIVE);

        given(integrationService.getIntegrationById(5L)).willReturn(response);

        mockMvc.perform(get("/api/integrations/connections/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.apiA").value("api-a"))
                .andExpect(jsonPath("$.apiB").value("api-b"));
    }

    @Test
    void updateIntegration_shouldReturn200() throws Exception {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("api-x");
        request.setApiB("api-y");

        IntegrationResponse response = new IntegrationResponse();
        response.setId(3L);
        response.setApiA("api-x");
        response.setApiB("api-y");

        given(integrationService.updateIntegration(eq(3L), any(IntegrationRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/integrations/connections/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.apiA").value("api-x"))
                .andExpect(jsonPath("$.apiB").value("api-y"));
    }

    @Test
    void updateIntegrationStatus_shouldReturn200() throws Exception {
        IntegrationResponse response = new IntegrationResponse();
        response.setId(4L);
        response.setStatus(IntegrationStatus.DELETED);

        given(integrationService.updateIntegrationStatus(eq(4L), eq(IntegrationStatus.DELETED))).willReturn(response);

        mockMvc.perform(patch("/api/integrations/connections/4/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(IntegrationStatus.DELETED)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    void deleteIntegration_shouldReturn204() throws Exception {
        willDoNothing().given(integrationService).deleteIntegration(7L);

        mockMvc.perform(delete("/api/integrations/connections/7"))
                .andExpect(status().isNoContent());
    }

    @Test
    void runMatching_shouldReturn200() throws Exception {
        Map<String, Object> result = Map.of("status", "completed", "score", 0.95);

        given(integrationService.runMatching(10L)).willReturn(result);

        mockMvc.perform(post("/api/integrations/connections/10/run-matching"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.score").value(0.95));
    }
}
