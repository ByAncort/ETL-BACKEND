package com.necronet.schemamatchingms.service;

import com.necronet.schemamatchingms.dto.MatchFeedbackRequestDTO;
import com.necronet.schemamatchingms.dto.SchemaMatchRequestDTO;
import com.necronet.schemamatchingms.entity.MatchFeedback;
import com.necronet.schemamatchingms.entity.MatchStatus;
import com.necronet.schemamatchingms.entity.SchemaMatch;
import com.necronet.schemamatchingms.repository.MatchFeedbackRepository;
import com.necronet.schemamatchingms.repository.SchemaMatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@SpringBootTest
class SchemaMatchServiceTest {

    @MockBean
    private SchemaMatchRepository schemaMatchRepository;

    @MockBean
    private MatchFeedbackRepository matchFeedbackRepository;

    @Autowired
    private SchemaMatchService schemaMatchService;

    private SchemaMatch createMatch(Long id, Long integrationId, String source, String target,
                                    BigDecimal confidence, MatchStatus status) {
        SchemaMatch match = new SchemaMatch(integrationId, source, target, confidence);
        match.setId(id);
        match.setStatus(status);
        return match;
    }

    @Test
    void getAllMatches_shouldReturnAll() {
        given(schemaMatchRepository.findAll())
                .willReturn(List.of(
                        createMatch(1L, 10L, "name", "full_name", new BigDecimal("0.9500"), MatchStatus.PENDING),
                        createMatch(2L, 10L, "email", "email_address", new BigDecimal("0.9800"), MatchStatus.ACCEPTED)
                ));

        List<SchemaMatch> results = schemaMatchService.getAllMatches();

        assertThat(results).hasSize(2);
        then(schemaMatchRepository).should(times(1)).findAll();
    }

    @Test
    void getMatchById_shouldReturnMatch() {
        SchemaMatch match = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(match));

        SchemaMatch result = schemaMatchService.getMatchById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getSourceField()).isEqualTo("name");
    }

    @Test
    void getMatchById_shouldThrowWhenNotFound() {
        given(schemaMatchRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> schemaMatchService.getMatchById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SchemaMatch not found with id: 99");
    }

    @Test
    void createMatch_shouldSucceed() {
        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();
        request.setIntegrationId(10L);
        request.setSourceField("name");
        request.setTargetField("full_name");
        request.setConfidence(new BigDecimal("0.9500"));

        given(schemaMatchRepository.existsSchemaMatch(10L, "name")).willReturn(false);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> {
            SchemaMatch saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        SchemaMatch result = schemaMatchService.createMatch(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSourceField()).isEqualTo("name");
        assertThat(result.getTargetField()).isEqualTo("full_name");
        assertThat(result.getConfidence()).isEqualByComparingTo(new BigDecimal("0.9500"));
        assertThat(result.getStatus()).isEqualTo(MatchStatus.PENDING);
        then(schemaMatchRepository).should(times(1)).save(any(SchemaMatch.class));
    }

    @Test
    void createMatch_shouldThrowWhenDuplicate() {
        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();
        request.setIntegrationId(10L);
        request.setSourceField("name");
        request.setTargetField("full_name");
        request.setConfidence(new BigDecimal("0.9500"));

        given(schemaMatchRepository.existsSchemaMatch(10L, "name")).willReturn(true);

        assertThatThrownBy(() -> schemaMatchService.createMatch(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
        then(schemaMatchRepository).should(never()).save(any(SchemaMatch.class));
    }

    @Test
    void getMatchesByIntegration_shouldReturnMatches() {
        given(schemaMatchRepository.findByIntegrationId(10L))
                .willReturn(List.of(
                        createMatch(1L, 10L, "name", "full_name", new BigDecimal("0.9500"), MatchStatus.PENDING)
                ));

        List<SchemaMatch> results = schemaMatchService.getMatchesByIntegration(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSourceField()).isEqualTo("name");
    }

    @Test
    void getMatchesByStatus_shouldReturnFiltered() {
        given(schemaMatchRepository.findByIntegrationIdAndStatus(10L, MatchStatus.PENDING))
                .willReturn(List.of(
                        createMatch(1L, 10L, "name", "full_name", new BigDecimal("0.9500"), MatchStatus.PENDING)
                ));

        List<SchemaMatch> results = schemaMatchService.getMatchesByStatus(10L, MatchStatus.PENDING);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(MatchStatus.PENDING);
    }

    @Test
    void updateMatch_shouldUpdateFields() {
        SchemaMatch existing = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(existing));

        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();
        request.setIntegrationId(10L);
        request.setSourceField("first_name");
        request.setTargetField("full_name");
        request.setConfidence(new BigDecimal("0.9900"));
        request.setStatus(MatchStatus.ACCEPTED);
        request.setTransformation("lowercase");
        request.setReviewedBy(42L);

        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        SchemaMatch result = schemaMatchService.updateMatch(1L, request);

        assertThat(result.getSourceField()).isEqualTo("first_name");
        assertThat(result.getConfidence()).isEqualByComparingTo(new BigDecimal("0.9900"));
        assertThat(result.getStatus()).isEqualTo(MatchStatus.ACCEPTED);
        assertThat(result.getTransformation()).isEqualTo("lowercase");
        assertThat(result.getReviewedBy()).isEqualTo(42L);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void updateMatchStatus_shouldUpdateStatus() {
        SchemaMatch existing = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(existing));
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        SchemaMatch result = schemaMatchService.updateMatchStatus(1L, MatchStatus.ACCEPTED, 42L);

        assertThat(result.getStatus()).isEqualTo(MatchStatus.ACCEPTED);
        assertThat(result.getReviewedBy()).isEqualTo(42L);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void deleteMatch_shouldDelete() {
        willDoNothing().given(schemaMatchRepository).deleteById(1L);

        schemaMatchService.deleteMatch(1L);

        then(schemaMatchRepository).should(times(1)).deleteById(1L);
    }

    @Test
    void addFeedback_shouldSaveAndUpdateMatch_whenApproved() {
        SchemaMatch pending = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(pending));

        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(true);
        request.setActualTarget("new_target");
        request.setReviewedBy(42L);

        MatchFeedback savedFeedback = new MatchFeedback(1L, true, "new_target");
        savedFeedback.setId(100L);
        given(matchFeedbackRepository.save(any(MatchFeedback.class))).willReturn(savedFeedback);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        MatchFeedback result = schemaMatchService.addFeedback(request);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUserApproved()).isTrue();
        assertThat(result.getActualTarget()).isEqualTo("new_target");
        assertThat(pending.getStatus()).isEqualTo(MatchStatus.ACCEPTED);
        assertThat(pending.getTargetField()).isEqualTo("new_target");
        assertThat(pending.getReviewedBy()).isEqualTo(42L);
        assertThat(pending.getReviewedAt()).isNotNull();
        then(matchFeedbackRepository).should(times(1)).save(any(MatchFeedback.class));
        then(schemaMatchRepository).should(times(1)).save(any(SchemaMatch.class));
    }

    @Test
    void addFeedback_shouldThrowWhenAlreadyReviewed() {
        SchemaMatch reviewed = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.ACCEPTED);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(reviewed));

        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(true);

        assertThatThrownBy(() -> schemaMatchService.addFeedback(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already been reviewed");
        then(matchFeedbackRepository).should(never()).save(any(MatchFeedback.class));
    }

    @Test
    void addFeedback_shouldRejectAndKeepTarget_whenNotApproved() {
        SchemaMatch pending = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(pending));

        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(false);
        request.setReviewedBy(42L);

        MatchFeedback savedFeedback = new MatchFeedback(1L, false, null);
        savedFeedback.setId(101L);
        given(matchFeedbackRepository.save(any(MatchFeedback.class))).willReturn(savedFeedback);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        MatchFeedback result = schemaMatchService.addFeedback(request);

        assertThat(result.getUserApproved()).isFalse();
        assertThat(pending.getStatus()).isEqualTo(MatchStatus.REJECTED);
        assertThat(pending.getTargetField()).isEqualTo("full_name");
    }

    @Test
    void getFeedbackByMatch_shouldReturnList() {
        given(matchFeedbackRepository.findByMatchId(1L))
                .willReturn(List.of(new MatchFeedback(1L, true, "target")));

        List<MatchFeedback> results = schemaMatchService.getFeedbackByMatch(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatchId()).isEqualTo(1L);
    }
}
