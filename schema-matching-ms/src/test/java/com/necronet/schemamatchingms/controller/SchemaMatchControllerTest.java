package com.necronet.schemamatchingms.controller;

import com.necronet.schemamatchingms.dto.MatchFeedbackRequestDTO;
import com.necronet.schemamatchingms.dto.MatchFeedbackResponseDTO;
import com.necronet.schemamatchingms.dto.SchemaMatchBatchRequestDTO;
import com.necronet.schemamatchingms.dto.SchemaMatchRequestDTO;
import com.necronet.schemamatchingms.entity.MatchFeedback;
import com.necronet.schemamatchingms.entity.MatchStatus;
import com.necronet.schemamatchingms.entity.SchemaMatch;
import com.necronet.schemamatchingms.service.SchemaMatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchemaMatchController.class)
class SchemaMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SchemaMatchService schemaMatchService;

    private SchemaMatch createMatch(Long id, Long integrationId, String source, String target,
                                    BigDecimal confidence, MatchStatus status) {
        SchemaMatch match = new SchemaMatch(integrationId, source, target, confidence);
        match.setId(id);
        match.setStatus(status);
        match.setCreatedAt(LocalDateTime.now());
        return match;
    }

    @Test
    void getAllMatches_shouldReturnList() throws Exception {
        List<SchemaMatch> matches = List.of(
                createMatch(1L, 10L, "name", "full_name", new BigDecimal("0.9500"), MatchStatus.PENDING),
                createMatch(2L, 10L, "email", "email_address", new BigDecimal("0.9800"), MatchStatus.ACCEPTED)
        );
        given(schemaMatchService.getAllMatches()).willReturn(matches);

        mockMvc.perform(get("/api/schema-matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sourceField", is("name")))
                .andExpect(jsonPath("$[0].targetField", is("full_name")))
                .andExpect(jsonPath("$[1].sourceField", is("email")));
    }

    @Test
    void getMatchesByIntegration_shouldReturnMatches() throws Exception {
        List<SchemaMatch> matches = List.of(
                createMatch(1L, 10L, "name", "full_name", new BigDecimal("0.9500"), MatchStatus.PENDING)
        );
        given(schemaMatchService.getMatchesByIntegration(10L)).willReturn(matches);

        mockMvc.perform(get("/api/schema-matches/integration/{integrationId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sourceField", is("name")));
    }

    @Test
    void getMatchesByStatus_shouldReturnFilteredMatches() throws Exception {
        List<SchemaMatch> matches = List.of(
                createMatch(1L, 10L, "name", "full_name", new BigDecimal("0.9500"), MatchStatus.PENDING)
        );
        given(schemaMatchService.getMatchesByStatus(10L, MatchStatus.PENDING)).willReturn(matches);

        mockMvc.perform(get("/api/schema-matches/integration/{integrationId}/status/{status}", 10L, "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    void getMatchById_shouldReturnMatch() throws Exception {
        SchemaMatch match = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchService.getMatchById(1L)).willReturn(match);

        mockMvc.perform(get("/api/schema-matches/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.sourceField", is("name")))
                .andExpect(jsonPath("$.targetField", is("full_name")))
                .andExpect(jsonPath("$.confidence", is(0.95)));
    }

    @Test
    void createMatch_shouldReturnCreated() throws Exception {
        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();
        request.setIntegrationId(10L);
        request.setSourceField("name");
        request.setTargetField("full_name");
        request.setConfidence(new BigDecimal("0.9500"));

        SchemaMatch saved = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchService.createMatch(any(SchemaMatchRequestDTO.class))).willReturn(saved);

        mockMvc.perform(post("/api/schema-matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.sourceField", is("name")));
    }

    @Test
    void createMatch_shouldReturnBadRequestWhenInvalid() throws Exception {
        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();

        mockMvc.perform(post("/api/schema-matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMatches_shouldReturnCreated() throws Exception {
        SchemaMatchRequestDTO req1 = new SchemaMatchRequestDTO();
        req1.setIntegrationId(10L);
        req1.setSourceField("name");
        req1.setTargetField("full_name");
        req1.setConfidence(new BigDecimal("0.9500"));

        SchemaMatchBatchRequestDTO batch = new SchemaMatchBatchRequestDTO();
        batch.setMatches(List.of(req1));

        SchemaMatch saved = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchService.createMatches(any())).willReturn(List.of(saved));

        mockMvc.perform(post("/api/schema-matches/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sourceField", is("name")));
    }

    @Test
    void updateMatch_shouldReturnUpdated() throws Exception {
        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();
        request.setIntegrationId(10L);
        request.setSourceField("first_name");
        request.setTargetField("full_name");
        request.setConfidence(new BigDecimal("0.9700"));

        SchemaMatch updated = createMatch(1L, 10L, "first_name", "full_name",
                new BigDecimal("0.9700"), MatchStatus.PENDING);
        given(schemaMatchService.updateMatch(eq(1L), any(SchemaMatchRequestDTO.class))).willReturn(updated);

        mockMvc.perform(put("/api/schema-matches/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceField", is("first_name")))
                .andExpect(jsonPath("$.confidence", is(0.97)));
    }

    @Test
    void updateMatchStatus_shouldReturnUpdated() throws Exception {
        SchemaMatch updated = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.ACCEPTED);
        given(schemaMatchService.updateMatchStatus(1L, MatchStatus.ACCEPTED, 42L)).willReturn(updated);

        mockMvc.perform(patch("/api/schema-matches/{id}/status", 1L)
                        .param("status", "ACCEPTED")
                        .param("reviewedBy", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));
    }

    @Test
    void updateMatchStatus_withoutReviewedBy_shouldReturnUpdated() throws Exception {
        SchemaMatch updated = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.REJECTED);
        given(schemaMatchService.updateMatchStatus(1L, MatchStatus.REJECTED, null)).willReturn(updated);

        mockMvc.perform(patch("/api/schema-matches/{id}/status", 1L)
                        .param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    @Test
    void deleteMatch_shouldReturnNoContent() throws Exception {
        willDoNothing().given(schemaMatchService).deleteMatch(1L);

        mockMvc.perform(delete("/api/schema-matches/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void addFeedback_shouldReturnCreated() throws Exception {
        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(true);
        request.setActualTarget("full_name");
        request.setReviewedBy(42L);

        MatchFeedback feedback = new MatchFeedback(1L, true, "full_name");
        feedback.setId(100L);
        given(schemaMatchService.addFeedback(any(MatchFeedbackRequestDTO.class))).willReturn(feedback);

        mockMvc.perform(post("/api/schema-matches/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.userApproved", is(true)))
                .andExpect(jsonPath("$.actualTarget", is("full_name")));
    }

    @Test
    void getFeedbackByMatch_shouldReturnList() throws Exception {
        MatchFeedback feedback1 = new MatchFeedback(1L, true, "full_name");
        feedback1.setId(100L);
        MatchFeedback feedback2 = new MatchFeedback(1L, false, null);
        feedback2.setId(101L);

        given(schemaMatchService.getFeedbackByMatch(1L)).willReturn(List.of(feedback1, feedback2));

        mockMvc.perform(get("/api/schema-matches/{id}/feedback", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userApproved", is(true)))
                .andExpect(jsonPath("$[1].userApproved", is(false)));
    }
}
