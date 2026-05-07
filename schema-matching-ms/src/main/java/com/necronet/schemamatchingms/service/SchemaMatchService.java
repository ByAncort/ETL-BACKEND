package com.necronet.schemamatchingms.service;

import com.necronet.schemamatchingms.dto.MatchFeedbackRequestDTO;
import com.necronet.schemamatchingms.dto.SchemaMatchRequestDTO;
import com.necronet.schemamatchingms.entity.MatchFeedback;
import com.necronet.schemamatchingms.entity.MatchStatus;
import com.necronet.schemamatchingms.entity.SchemaMatch;
import com.necronet.schemamatchingms.repository.MatchFeedbackRepository;
import com.necronet.schemamatchingms.repository.SchemaMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SchemaMatchService {

    private final SchemaMatchRepository schemaMatchRepository;
    private final MatchFeedbackRepository matchFeedbackRepository;

    public SchemaMatchService(SchemaMatchRepository schemaMatchRepository,
                              MatchFeedbackRepository matchFeedbackRepository) {
        this.schemaMatchRepository = schemaMatchRepository;
        this.matchFeedbackRepository = matchFeedbackRepository;
    }

    public List<SchemaMatch> getAllMatches() {
        return schemaMatchRepository.findAll();
    }

    public List<SchemaMatch> getMatchesByIntegration(Long integrationId) {
        return schemaMatchRepository.findByIntegrationId(integrationId);
    }

    public List<SchemaMatch> getMatchesByStatus(Long integrationId, MatchStatus status) {
        return schemaMatchRepository.findByIntegrationIdAndStatus(integrationId, status);
    }

    public SchemaMatch getMatchById(Long id) {
        return schemaMatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SchemaMatch not found with id: " + id));
    }

    @Transactional
    public SchemaMatch createMatch(SchemaMatchRequestDTO request) {
        SchemaMatch match = new SchemaMatch(
                request.getIntegrationId(),
                request.getSourceField(),
                request.getTargetField(),
                request.getConfidence()
        );
        if (request.getStatus() != null) {
            match.setStatus(request.getStatus());
        }
        if (request.getTransformation() != null) {
            match.setTransformation(request.getTransformation());
        }
        return schemaMatchRepository.save(match);
    }

    @Transactional
    public SchemaMatch updateMatch(Long id, SchemaMatchRequestDTO request) {
        SchemaMatch match = getMatchById(id);
        match.setSourceField(request.getSourceField());
        match.setTargetField(request.getTargetField());
        match.setConfidence(request.getConfidence());
        if (request.getStatus() != null) {
            match.setStatus(request.getStatus());
        }
        if (request.getTransformation() != null) {
            match.setTransformation(request.getTransformation());
        }
        if (request.getReviewedBy() != null) {
            match.setReviewedBy(request.getReviewedBy());
            match.setReviewedAt(LocalDateTime.now());
        }
        return schemaMatchRepository.save(match);
    }

    @Transactional
    public SchemaMatch updateMatchStatus(Long id, MatchStatus status, Long reviewedBy) {
        SchemaMatch match = getMatchById(id);
        match.setStatus(status);
        match.setReviewedBy(reviewedBy);
        match.setReviewedAt(LocalDateTime.now());
        return schemaMatchRepository.save(match);
    }

    @Transactional
    public void deleteMatch(Long id) {
        schemaMatchRepository.deleteById(id);
    }

    @Transactional
    public MatchFeedback addFeedback(MatchFeedbackRequestDTO request) {
        MatchFeedback feedback = new MatchFeedback(
                request.getMatchId(),
                request.getUserApproved(),
                request.getActualTarget()
        );
        return matchFeedbackRepository.save(feedback);
    }

    public List<MatchFeedback> getFeedbackByMatch(Long matchId) {
        return matchFeedbackRepository.findByMatchId(matchId);
    }
}