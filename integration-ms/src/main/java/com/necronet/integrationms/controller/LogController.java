package com.necronet.integrationms.controller;

import com.necronet.integrationms.dto.LogRequest;
import com.necronet.integrationms.entity.ExecutionLog;
import com.necronet.integrationms.entity.LogLevel;
import com.necronet.integrationms.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/logs")
@RequiredArgsConstructor
public class LogController {

    private final ExecutionLogRepository logRepository;

    @PostMapping
    public ResponseEntity<Void> saveLog(@RequestBody LogRequest request) {
        ExecutionLog log = ExecutionLog.builder()
                .id(UUID.randomUUID().toString())
                .parentId(request.getParentId())
                .executionId(request.getExecutionId() != null ? request.getExecutionId() : UUID.randomUUID().toString())
                .serviceName(request.getServiceName() != null ? request.getServiceName() : "ms-save-data")
                .className(request.getClassName())
                .methodName(request.getMethodName())
                .logLevel(LogLevel.valueOf(request.getLogLevel().toUpperCase()))
                .message(request.getMessage())
                .detail(request.getDetail())
                .timestamp(LocalDateTime.now())
                .durationMs(request.getDurationMs())
                .integrationId(request.getIntegrationId())
                .build();

        logRepository.save(log);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<ExecutionLog>> getLogs(
            @RequestParam(required = false) String integrationId,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String logLevel) {

        if (integrationId != null) {
            return ResponseEntity.ok(logRepository.findByIntegrationIdOrderByTimestampDesc(integrationId));
        }
        if (executionId != null) {
            return ResponseEntity.ok(logRepository.findByExecutionIdOrderByTimestampAsc(executionId));
        }
        if (logLevel != null) {
            return ResponseEntity.ok(logRepository.findByLogLevelOrderByTimestampDesc(LogLevel.valueOf(logLevel.toUpperCase())));
        }
        return ResponseEntity.ok(logRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExecutionLog> getLogById(@PathVariable String id) {
        Optional<ExecutionLog> log = logRepository.findById(id);
        return log.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
