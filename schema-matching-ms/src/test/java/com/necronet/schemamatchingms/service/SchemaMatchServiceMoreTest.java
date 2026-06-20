package com.necronet.schemamatchingms.service;

import com.necronet.schemamatchingms.dto.MatchFeedbackRequestDTO;
import com.necronet.schemamatchingms.dto.SchemaMatchRequestDTO;
import com.necronet.schemamatchingms.entity.MatchFeedback;
import com.necronet.schemamatchingms.entity.MatchStatus;
import com.necronet.schemamatchingms.entity.SchemaMatch;
import com.necronet.schemamatchingms.repository.MatchFeedbackRepository;
import com.necronet.schemamatchingms.repository.SchemaMatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SchemaMatchServiceMoreTest {

    @Mock
    private SchemaMatchRepository schemaMatchRepository;

    @Mock
    private MatchFeedbackRepository matchFeedbackRepository;

    @InjectMocks
    private SchemaMatchService schemaMatchService;

    @Captor
    private ArgumentCaptor<SchemaMatch> schemaMatchCaptor;

    @Captor
    private ArgumentCaptor<MatchFeedback> feedbackCaptor;

    private SchemaMatch createMatch(Long id, Long integrationId, String source, String target,
                                    BigDecimal confidence, MatchStatus status) {
        SchemaMatch match = new SchemaMatch(integrationId, source, target, confidence);
        match.setId(id);
        match.setStatus(status);
        return match;
    }

    @Test
    void createMatch_whenDuplicateExists_shouldThrow() {
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
    void createMatch_withNullStatusAndTransformation_shouldDefaultToPendingAndNull() {
        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();
        request.setIntegrationId(20L);
        request.setSourceField("email");
        request.setTargetField("email_addr");
        request.setConfidence(new BigDecimal("0.9000"));

        given(schemaMatchRepository.existsSchemaMatch(20L, "email")).willReturn(false);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> {
            SchemaMatch saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        SchemaMatch result = schemaMatchService.createMatch(request);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo(MatchStatus.PENDING);
        assertThat(result.getTransformation()).isNull();
        then(schemaMatchRepository).should(times(1)).save(any(SchemaMatch.class));
    }

    @Test
    void createMatch_withCustomStatusAndTransformation_shouldApply() {
        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();
        request.setIntegrationId(30L);
        request.setSourceField("phone");
        request.setTargetField("phone_number");
        request.setConfidence(new BigDecimal("0.8500"));
        request.setStatus(MatchStatus.ACCEPTED);
        request.setTransformation("strip_dashes");

        given(schemaMatchRepository.existsSchemaMatch(30L, "phone")).willReturn(false);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> {
            SchemaMatch saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        SchemaMatch result = schemaMatchService.createMatch(request);

        assertThat(result.getStatus()).isEqualTo(MatchStatus.ACCEPTED);
        assertThat(result.getTransformation()).isEqualTo("strip_dashes");
    }

    @Test
    void addFeedback_whenAlreadyReviewedWithRejectedStatus_shouldThrow() {
        SchemaMatch reviewed = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.REJECTED);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(reviewed));

        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(true);
        request.setReviewedBy(42L);

        assertThatThrownBy(() -> schemaMatchService.addFeedback(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already been reviewed");
        then(matchFeedbackRepository).should(never()).save(any(MatchFeedback.class));
    }

    @Test
    void addFeedback_whenUserApprovedFalseAndActualTargetSet_shouldUpdateTargetField() {
        SchemaMatch pending = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(pending));

        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(false);
        request.setActualTarget("corrected_name");
        request.setReviewedBy(42L);

        MatchFeedback savedFeedback = new MatchFeedback(1L, false, "corrected_name");
        savedFeedback.setId(200L);
        given(matchFeedbackRepository.save(any(MatchFeedback.class))).willReturn(savedFeedback);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        MatchFeedback result = schemaMatchService.addFeedback(request);

        assertThat(result.getUserApproved()).isFalse();
        assertThat(pending.getStatus()).isEqualTo(MatchStatus.REJECTED);
        assertThat(pending.getTargetField()).isEqualTo("corrected_name");
    }

    @Test
    void addFeedback_whenUserApprovedFalseAndActualTargetNull_shouldKeepOriginalTarget() {
        SchemaMatch pending = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(pending));

        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(false);
        request.setActualTarget(null);
        request.setReviewedBy(42L);

        MatchFeedback savedFeedback = new MatchFeedback(1L, false, null);
        savedFeedback.setId(201L);
        given(matchFeedbackRepository.save(any(MatchFeedback.class))).willReturn(savedFeedback);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        schemaMatchService.addFeedback(request);

        assertThat(pending.getTargetField()).isEqualTo("full_name");
        assertThat(pending.getStatus()).isEqualTo(MatchStatus.REJECTED);
    }

    @Test
    void addFeedback_whenUserApprovedFalseAndActualTargetBlank_shouldKeepOriginalTarget() {
        SchemaMatch pending = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(pending));

        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(false);
        request.setActualTarget("   ");
        request.setReviewedBy(42L);

        MatchFeedback savedFeedback = new MatchFeedback(1L, false, "   ");
        savedFeedback.setId(202L);
        given(matchFeedbackRepository.save(any(MatchFeedback.class))).willReturn(savedFeedback);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        schemaMatchService.addFeedback(request);

        assertThat(pending.getTargetField()).isEqualTo("full_name");
        assertThat(pending.getStatus()).isEqualTo(MatchStatus.REJECTED);
    }

    @Test
    void addFeedback_whenUserApprovedTrueAndActualTargetNull_shouldKeepOriginalTarget() {
        SchemaMatch pending = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(pending));

        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(true);
        request.setActualTarget(null);
        request.setReviewedBy(42L);

        MatchFeedback savedFeedback = new MatchFeedback(1L, true, null);
        savedFeedback.setId(203L);
        given(matchFeedbackRepository.save(any(MatchFeedback.class))).willReturn(savedFeedback);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        schemaMatchService.addFeedback(request);

        assertThat(pending.getTargetField()).isEqualTo("full_name");
        assertThat(pending.getStatus()).isEqualTo(MatchStatus.ACCEPTED);
    }

    @Test
    void updateMatch_whenReviewedByProvided_shouldSetReviewedAt() {
        SchemaMatch existing = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(existing));

        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();
        request.setIntegrationId(10L);
        request.setSourceField("name");
        request.setTargetField("updated_target");
        request.setConfidence(new BigDecimal("0.9700"));
        request.setReviewedBy(100L);

        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        SchemaMatch result = schemaMatchService.updateMatch(1L, request);

        assertThat(result.getReviewedBy()).isEqualTo(100L);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void updateMatch_whenReviewedByNotProvided_shouldNotSetReviewedAt() {
        SchemaMatch existing = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(existing));

        SchemaMatchRequestDTO request = new SchemaMatchRequestDTO();
        request.setIntegrationId(10L);
        request.setSourceField("name");
        request.setTargetField("updated_target");
        request.setConfidence(new BigDecimal("0.9700"));

        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        SchemaMatch result = schemaMatchService.updateMatch(1L, request);

        assertThat(result.getReviewedBy()).isNull();
        assertThat(result.getReviewedAt()).isNull();
    }

    @Test
    void updateMatchStatus_withNullReviewedBy_shouldSetReviewedByToNull() {
        SchemaMatch existing = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(existing));
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        SchemaMatch result = schemaMatchService.updateMatchStatus(1L, MatchStatus.REJECTED, null);

        assertThat(result.getStatus()).isEqualTo(MatchStatus.REJECTED);
        assertThat(result.getReviewedBy()).isNull();
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void getMatchesByStatus_whenNoneMatch_shouldReturnEmpty() {
        given(schemaMatchRepository.findByIntegrationIdAndStatus(99L, MatchStatus.PENDING))
                .willReturn(List.of());

        List<SchemaMatch> results = schemaMatchService.getMatchesByStatus(99L, MatchStatus.PENDING);

        assertThat(results).isEmpty();
    }

    @Test
    void deleteMatch_whenIdDoesNotExist_shouldNotThrow() {
        schemaMatchService.deleteMatch(999L);

        then(schemaMatchRepository).should(times(1)).deleteById(999L);
    }

    @Test
    void getFeedbackByMatch_whenNoFeedback_shouldReturnEmpty() {
        given(matchFeedbackRepository.findByMatchId(777L)).willReturn(List.of());

        List<MatchFeedback> results = schemaMatchService.getFeedbackByMatch(777L);

        assertThat(results).isEmpty();
    }

    @Test
    void addFeedback_whenReviewedByProvided_shouldSetReviewedByOnMatch() {
        SchemaMatch pending = createMatch(1L, 10L, "name", "full_name",
                new BigDecimal("0.9500"), MatchStatus.PENDING);
        given(schemaMatchRepository.findById(1L)).willReturn(Optional.of(pending));

        MatchFeedbackRequestDTO request = new MatchFeedbackRequestDTO();
        request.setMatchId(1L);
        request.setUserApproved(true);
        request.setActualTarget("corrected");
        request.setReviewedBy(77L);

        MatchFeedback savedFeedback = new MatchFeedback(1L, true, "corrected");
        savedFeedback.setId(300L);
        given(matchFeedbackRepository.save(any(MatchFeedback.class))).willReturn(savedFeedback);
        given(schemaMatchRepository.save(any(SchemaMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

        schemaMatchService.addFeedback(request);

        assertThat(pending.getReviewedBy()).isEqualTo(77L);
        assertThat(pending.getReviewedAt()).isNotNull();
    }
}
