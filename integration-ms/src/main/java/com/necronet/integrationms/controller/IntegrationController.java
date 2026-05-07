package com.necronet.integrationms.controller;

import com.necronet.integrationms.dto.IntegrationRequest;
import com.necronet.integrationms.dto.IntegrationResponse;
import com.necronet.integrationms.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    @PostMapping("/connections")
    public ResponseEntity<IntegrationResponse> createIntegration(@RequestBody IntegrationRequest request) {
        IntegrationResponse response = integrationService.createIntegration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/connections")
    public ResponseEntity<List<IntegrationResponse>> getAllIntegrations() {
        List<IntegrationResponse> responses = integrationService.getAllIntegrations();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/connections/{id}")
    public ResponseEntity<IntegrationResponse> getIntegrationById(@PathVariable Long id) {
        IntegrationResponse response = integrationService.getIntegrationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/connections/{id}")
    public ResponseEntity<IntegrationResponse> updateIntegration(@PathVariable Long id, @RequestBody IntegrationRequest request) {
        IntegrationResponse response = integrationService.updateIntegration(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> deleteIntegration(@PathVariable Long id) {
        integrationService.deleteIntegration(id);
        return ResponseEntity.noContent().build();
    }
}