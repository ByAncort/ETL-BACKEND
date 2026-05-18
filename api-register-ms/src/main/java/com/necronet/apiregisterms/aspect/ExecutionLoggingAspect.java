package com.necronet.apiregisterms.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.necronet.apiregisterms.entity.ExecutionLog;
import com.necronet.apiregisterms.entity.LogLevel;
import com.necronet.apiregisterms.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionLoggingAspect {

    private final ExecutionLogRepository logRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Around("execution(* com.necronet.apiregisterms.service.*.*(..))")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint);
    }

    @Around("execution(* com.necronet.apiregisterms.controller.*.*(..))")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint);
    }

    private Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String executionId = UUID.randomUUID().toString();
        String parentId = UUID.randomUUID().toString();
        String integrationId = extractIntegrationId(joinPoint.getArgs());

        ExecutionLog parent = ExecutionLog.builder()
                .id(parentId)
                .executionId(executionId)
                .serviceName(serviceName)
                .className(className)
                .methodName(methodName)
                .logLevel(LogLevel.INFO)
                .message("Executing " + className + "." + methodName)
                .timestamp(LocalDateTime.now())
                .integrationId(integrationId)
                .build();

        try {
            logRepository.save(parent);
        } catch (Exception e) {
            log.trace("Could not save parent log", e);
        }

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            try {
                parent.setDurationMs(duration);
                parent.setLogLevel(LogLevel.INFO);
                parent.setMessage("Completed " + className + "." + methodName + " (" + duration + "ms)");
                logRepository.save(parent);
            } catch (Exception e) {
                log.trace("Could not update parent log", e);
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            String detailJson = buildErrorDetail(e, joinPoint.getArgs());

            ExecutionLog child = ExecutionLog.builder()
                    .id(UUID.randomUUID().toString())
                    .parentId(parentId)
                    .executionId(executionId)
                    .serviceName(serviceName)
                    .className(className)
                    .methodName(methodName)
                    .logLevel(LogLevel.ERROR)
                    .message("Error in " + className + "." + methodName + ": " + e.getClass().getSimpleName() + " - " + e.getMessage())
                    .detail(detailJson)
                    .timestamp(LocalDateTime.now())
                    .integrationId(integrationId)
                    .build();

            try {
                logRepository.save(child);
            } catch (Exception ex) {
                log.trace("Could not save error child log", ex);
            }

            try {
                parent.setDurationMs(duration);
                parent.setLogLevel(LogLevel.ERROR);
                parent.setMessage("Failed " + className + "." + methodName + " (" + duration + "ms)");
                logRepository.save(parent);
            } catch (Exception ex) {
                log.trace("Could not update parent log on error", ex);
            }

            throw e;
        }
    }

    private String buildErrorDetail(Exception e, Object[] args) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("exception", e.getClass().getName());
            detail.put("message", e.getMessage());

            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                stackTrace.append(element.toString()).append("\n");
            }
            detail.put("stackTrace", stackTrace.toString());

            if (e.getCause() != null) {
                Map<String, Object> cause = new LinkedHashMap<>();
                cause.put("exception", e.getCause().getClass().getName());
                cause.put("message", e.getCause().getMessage());
                StringBuilder causeTrace = new StringBuilder();
                for (StackTraceElement element : e.getCause().getStackTrace()) {
                    causeTrace.append(element.toString()).append("\n");
                }
                cause.put("stackTrace", causeTrace.toString());
                detail.put("cause", cause);
            }

            if (args != null && args.length > 0) {
                String[] argStrings = new String[args.length];
                for (int i = 0; i < args.length; i++) {
                    try {
                        argStrings[i] = objectMapper.writeValueAsString(args[i]);
                    } catch (Exception ex) {
                        argStrings[i] = String.valueOf(args[i]);
                    }
                }
                detail.put("args", argStrings);
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            return "{\"error\": \"Failed to serialize error details\"}";
        }
    }

    private String extractIntegrationId(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Long id) {
                    return id.toString();
                }
                if (arg instanceof String str) {
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
