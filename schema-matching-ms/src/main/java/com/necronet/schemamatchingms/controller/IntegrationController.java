package com.necronet.schemamatchingms.controller;

import com.necronet.schemamatchingms.dto.ConnectionResponseDTO;
import com.necronet.schemamatchingms.service.IntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping("/connections/{connectionId}")
    public ResponseEntity<ConnectionResponseDTO> getConnection(@PathVariable Long connectionId) {
        ConnectionResponseDTO connection = integrationService.getConnection(connectionId);
        return ResponseEntity.ok(connection);
    }
}