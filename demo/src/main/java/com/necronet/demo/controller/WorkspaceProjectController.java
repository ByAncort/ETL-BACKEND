package com.necronet.demo.controller;

import com.necronet.demo.dto.ProjectCreateRequest;
import com.necronet.demo.dto.ProjectResponse;
import com.necronet.demo.service.WorkspaceProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspace-projects")
public class WorkspaceProjectController {

    private final WorkspaceProjectService service;

    public WorkspaceProjectController(WorkspaceProjectService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{code}")
    public ResponseEntity<ProjectResponse> findByCode(@PathVariable String code) {
        ProjectResponse response = service.findByCode(code);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
