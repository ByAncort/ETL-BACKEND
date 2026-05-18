package com.necronet.integrationms.service;

import com.necronet.integrationms.entity.ExecutionLog;
import com.necronet.integrationms.entity.LogLevel;
import com.necronet.integrationms.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final ExecutionLogRepository logRepository;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    public ExecutionLog createParentLog(String className, String methodName, String message, Object... context) {
        try {
            ExecutionLog executionLog = ExecutionLog.builder()
                    .id(UUID.randomUUID().toString())
                    .executionId(UUID.randomUUID().toString())
                    .parentId(null)
                    .serviceName(serviceName)
                    .className(className)
                    .methodName(methodName)
                    .logLevel(LogLevel.INFO)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .integrationId(extractIntegrationId(context))
                    .build();
            return logRepository.save(executionLog);
        } catch (Exception e) {
            log.warn("Failed to save parent log", e);
            return null;
        }
    }

    public ExecutionLog createChildLog(String parentId, String executionId, String className,
                                        String methodName, LogLevel level, String message,
                                        String detail, Object... context) {
        try {
            ExecutionLog child = ExecutionLog.builder()
                    .id(UUID.randomUUID().toString())
                    .parentId(parentId)
                    .executionId(executionId)
                    .serviceName(serviceName)
                    .className(className)
                    .methodName(methodName)
                    .logLevel(level)
                    .message(message)
                    .detail(detail)
                    .timestamp(LocalDateTime.now())
                    .integrationId(extractIntegrationId(context))
                    .build();
            return logRepository.save(child);
        } catch (Exception e) {
            log.warn("Failed to save child log", e);
            return null;
        }
    }

    public void updateParentLog(String id, LogLevel level, String message, Long durationMs) {
        try {
            logRepository.findById(id).ifPresent(logEntry -> {
                logEntry.setLogLevel(level);
                logEntry.setMessage(message);
                logEntry.setDurationMs(durationMs);
                logRepository.save(logEntry);
            });
        } catch (Exception e) {
            log.warn("Failed to update parent log", e);
        }
    }

    public void saveLogAsync(ExecutionLog executionLog) {
        try {
            logRepository.save(executionLog);
        } catch (Exception e) {
            log.warn("Failed to save async log", e);
        }
    }

    private String extractIntegrationId(Object... context) {
        if (context != null) {
            for (Object obj : context) {
                if (obj instanceof String str) {
                    try {
                        Long.parseLong(str);
                        return str;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }
}
