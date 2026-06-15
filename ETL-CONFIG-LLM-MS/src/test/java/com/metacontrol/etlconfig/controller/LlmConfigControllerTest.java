package com.metacontrol.etlconfig.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacontrol.etlconfig.dto.LlmConfigRequest;
import com.metacontrol.etlconfig.dto.LlmConfigResponse;
import com.metacontrol.etlconfig.entity.LlmStatus;
import com.metacontrol.etlconfig.service.LlmConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LlmConfigController.class)
class LlmConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LlmConfigService service;

    private final LocalDateTime now = LocalDateTime.now();

    private LlmConfigResponse buildResponse(Long id, String name, Boolean isDefault) {
        LlmConfigResponse resp = new LlmConfigResponse();
        resp.setId(id);
        resp.setName(name);
        resp.setProvider("openai");
        resp.setApiKey("sk-xxx");
        resp.setBaseUrl("https://api.openai.com");
        resp.setModelName("gpt-4");
        resp.setIsDefault(isDefault);
        resp.setStatus(LlmStatus.active);
        resp.setCreatedAt(now);
        resp.setUpdatedAt(now);
        return resp;
    }

    @Test
    void createShouldReturn201() throws Exception {
        LlmConfigRequest request = new LlmConfigRequest();
        request.setName("my-config");
        request.setProvider("openai");
        request.setApiKey("sk-xxx");
        request.setBaseUrl("https://api.openai.com");
        request.setModelName("gpt-4");
        request.setIsDefault(false);

        LlmConfigResponse response = buildResponse(1L, "my-config", false);
        given(service.create(any(LlmConfigRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/llm-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("my-config"))
                .andExpect(jsonPath("$.provider").value("openai"));
    }

    @Test
    void createShouldReturn400WhenInvalid() throws Exception {
        LlmConfigRequest request = new LlmConfigRequest();
        request.setName("");

        mockMvc.perform(post("/api/llm-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdShouldReturn200() throws Exception {
        LlmConfigResponse response = buildResponse(1L, "my-config", false);
        given(service.getById(1L)).willReturn(response);

        mockMvc.perform(get("/api/llm-configs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("my-config"));
    }

    @Test
    void getAllShouldReturn200() throws Exception {
        LlmConfigResponse r1 = buildResponse(1L, "cfg-1", false);
        LlmConfigResponse r2 = buildResponse(2L, "cfg-2", true);
        given(service.getAll()).willReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/llm-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("cfg-1"))
                .andExpect(jsonPath("$[1].name").value("cfg-2"));
    }

    @Test
    void getDefaultShouldReturn200() throws Exception {
        LlmConfigResponse response = buildResponse(1L, "default-cfg", true);
        given(service.getDefault()).willReturn(response);

        mockMvc.perform(get("/api/llm-configs/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("default-cfg"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void updateShouldReturn200() throws Exception {
        LlmConfigRequest request = new LlmConfigRequest();
        request.setName("updated-config");
        request.setProvider("anthropic");
        request.setApiKey("sk-anthropic");
        request.setBaseUrl("https://api.anthropic.com");
        request.setModelName("claude-3");
        request.setIsDefault(true);

        LlmConfigResponse response = buildResponse(1L, "updated-config", true);
        response.setProvider("anthropic");
        given(service.update(eq(1L), any(LlmConfigRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/llm-configs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated-config"))
                .andExpect(jsonPath("$.provider").value("anthropic"));
    }

    @Test
    void deleteShouldReturn204() throws Exception {
        willDoNothing().given(service).delete(1L);

        mockMvc.perform(delete("/api/llm-configs/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void setDefaultShouldReturn200() throws Exception {
        LlmConfigResponse response = buildResponse(2L, "new-default", true);
        given(service.setDefault(2L)).willReturn(response);

        mockMvc.perform(patch("/api/llm-configs/2/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.isDefault").value(true));
    }
}
