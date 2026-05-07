package com.necronet.schemamatchingms.controller;

import com.necronet.schemamatchingms.dto.MatchFeedbackRequestDTO;
import com.necronet.schemamatchingms.dto.MatchFeedbackResponseDTO;
import com.necronet.schemamatchingms.dto.SchemaMatchRequestDTO;
import com.necronet.schemamatchingms.dto.SchemaMatchResponseDTO;
import com.necronet.schemamatchingms.entity.MatchFeedback;
import com.necronet.schemamatchingms.entity.MatchStatus;
import com.necronet.schemamatchingms.entity.SchemaMatch;
import com.necronet.schemamatchingms.service.SchemaMatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schema-matches")
public class SchemaMatchController {

    private final SchemaMatchService schemaMatchService;

    public SchemaMatchController(SchemaMatchService schemaMatchService) {
        this.schemaMatchService = schemaMatchService;
    }

    @GetMapping
    public ResponseEntity<List<SchemaMatchResponseDTO>> getAllMatches() {
        List<SchemaMatchResponseDTO> matches = schemaMatchService.getAllMatches().stream()
                .map(SchemaMatchResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/integration/{integrationId}")
    public ResponseEntity<List<SchemaMatchResponseDTO>> getMatchesByIntegration(
            @PathVariable Long integrationId) {
        List<SchemaMatchResponseDTO> matches = schemaMatchService.getMatchesByIntegration(integrationId).stream()
                .map(SchemaMatchResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/integration/{integrationId}/status/{status}")
    public ResponseEntity<List<SchemaMatchResponseDTO>> getMatchesByStatus(
            @PathVariable Long integrationId,
            @PathVariable MatchStatus status) {
        List<SchemaMatchResponseDTO> matches = schemaMatchService.getMatchesByStatus(integrationId, status).stream()
                .map(SchemaMatchResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SchemaMatchResponseDTO> getMatchById(@PathVariable Long id) {
        SchemaMatch match = schemaMatchService.getMatchById(id);
        return ResponseEntity.ok(SchemaMatchResponseDTO.fromEntity(match));
    }

    @PostMapping
    public ResponseEntity<SchemaMatchResponseDTO> createMatch(
            @Valid @RequestBody SchemaMatchRequestDTO request) {
        SchemaMatch match = schemaMatchService.createMatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SchemaMatchResponseDTO.fromEntity(match));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SchemaMatchResponseDTO> updateMatch(
            @PathVariable Long id,
            @Valid @RequestBody SchemaMatchRequestDTO request) {
        SchemaMatch match = schemaMatchService.updateMatch(id, request);
        return ResponseEntity.ok(SchemaMatchResponseDTO.fromEntity(match));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SchemaMatchResponseDTO> updateMatchStatus(
            @PathVariable Long id,
            @RequestParam MatchStatus status,
            @RequestParam(required = false) Long reviewedBy) {
        SchemaMatch match = schemaMatchService.updateMatchStatus(id, status, reviewedBy);
        return ResponseEntity.ok(SchemaMatchResponseDTO.fromEntity(match));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        schemaMatchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/feedback")
    public ResponseEntity<MatchFeedbackResponseDTO> addFeedback(
            @Valid @RequestBody MatchFeedbackRequestDTO request) {
        MatchFeedback feedback = schemaMatchService.addFeedback(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MatchFeedbackResponseDTO.fromEntity(feedback));
    }

    @GetMapping("/{id}/feedback")
    public ResponseEntity<List<MatchFeedbackResponseDTO>> getFeedbackByMatch(@PathVariable Long id) {
        List<MatchFeedbackResponseDTO> feedback = schemaMatchService.getFeedbackByMatch(id).stream()
                .map(MatchFeedbackResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(feedback);
    }
}