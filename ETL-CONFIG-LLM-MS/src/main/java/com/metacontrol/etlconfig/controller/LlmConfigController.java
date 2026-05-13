package com.metacontrol.etlconfig.controller;

import com.metacontrol.etlconfig.dto.LlmConfigRequest;
import com.metacontrol.etlconfig.dto.LlmConfigResponse;
import com.metacontrol.etlconfig.service.LlmConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/llm-configs")
@RequiredArgsConstructor
public class LlmConfigController {

    private final LlmConfigService service;

    @PostMapping
    public ResponseEntity<LlmConfigResponse> create(@Valid @RequestBody LlmConfigRequest request) {
        LlmConfigResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LlmConfigResponse> getById(@PathVariable Long id) {
        LlmConfigResponse response = service.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<LlmConfigResponse>> getAll() {
        List<LlmConfigResponse> configs = service.getAll();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/default")
    public ResponseEntity<LlmConfigResponse> getDefault() {
        LlmConfigResponse response = service.getDefault();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LlmConfigResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LlmConfigRequest request) {
        LlmConfigResponse response = service.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<LlmConfigResponse> setDefault(@PathVariable Long id) {
        LlmConfigResponse response = service.setDefault(id);
        return ResponseEntity.ok(response);
    }
}
