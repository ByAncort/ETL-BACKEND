package com.necronet.apiregisterms.service;

import com.necronet.apiregisterms.entity.ExecutionLog;
import com.necronet.apiregisterms.entity.LogLevel;
import com.necronet.apiregisterms.repository.ExecutionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private ExecutionLogRepository executionLogRepository;

    @InjectMocks
    private LogService logService;

    @Captor
    private ArgumentCaptor<ExecutionLog> executionLogCaptor;

    @Test
    void createParentLog_shouldSaveAndReturnLog() {
        ExecutionLog savedLog = ExecutionLog.builder()
                .id("parent-id-1")
                .executionId("exec-id-1")
                .serviceName("api-register-ms")
                .className("TestClass")
                .methodName("testMethod")
                .logLevel(LogLevel.INFO)
                .message("Parent log message")
                .build();

        given(executionLogRepository.save(any(ExecutionLog.class))).willReturn(savedLog);

        ExecutionLog result = logService.createParentLog("TestClass", "testMethod", "Parent log message", "123");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("parent-id-1");
        assertThat(result.getExecutionId()).isEqualTo("exec-id-1");
        assertThat(result.getParentId()).isNull();
        assertThat(result.getServiceName()).isEqualTo("api-register-ms");
        assertThat(result.getClassName()).isEqualTo("TestClass");
        assertThat(result.getMethodName()).isEqualTo("testMethod");
        assertThat(result.getLogLevel()).isEqualTo(LogLevel.INFO);
        assertThat(result.getMessage()).isEqualTo("Parent log message");

        then(executionLogRepository).should(times(1)).save(any(ExecutionLog.class));
    }

    @Test
    void createParentLog_withContext_shouldExtractIntegrationId() {
        given(executionLogRepository.save(any(ExecutionLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        ExecutionLog result = logService.createParentLog("Service", "process", "Processing", "42", "extra");

        assertThat(result.getIntegrationId()).isEqualTo("42");
    }

    @Test
    void createParentLog_withNonNumericContext_shouldNotSetIntegrationId() {
        given(executionLogRepository.save(any(ExecutionLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        ExecutionLog result = logService.createParentLog("Service", "process", "Processing", "abc", "extra");

        assertThat(result.getIntegrationId()).isNull();
    }

    @Test
    void createParentLog_whenRepositoryThrows_shouldReturnNull() {
        given(executionLogRepository.save(any(ExecutionLog.class))).willThrow(new RuntimeException("DB error"));

        ExecutionLog result = logService.createParentLog("TestClass", "testMethod", "message");

        assertThat(result).isNull();
    }

    @Test
    void createChildLog_shouldSaveAndReturnLog() {
        ExecutionLog savedChild = ExecutionLog.builder()
                .id("child-id-1")
                .parentId("parent-id-1")
                .executionId("exec-id-1")
                .serviceName("api-register-ms")
                .className("ChildClass")
                .methodName("childMethod")
                .logLevel(LogLevel.ERROR)
                .message("Child error message")
                .detail("Stack trace detail")
                .build();

        given(executionLogRepository.save(any(ExecutionLog.class))).willReturn(savedChild);

        ExecutionLog result = logService.createChildLog(
                "parent-id-1", "exec-id-1", "ChildClass", "childMethod",
                LogLevel.ERROR, "Child error message", "Stack trace detail", "456");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("child-id-1");
        assertThat(result.getParentId()).isEqualTo("parent-id-1");
        assertThat(result.getExecutionId()).isEqualTo("exec-id-1");
        assertThat(result.getClassName()).isEqualTo("ChildClass");
        assertThat(result.getMethodName()).isEqualTo("childMethod");
        assertThat(result.getLogLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(result.getMessage()).isEqualTo("Child error message");
        assertThat(result.getDetail()).isEqualTo("Stack trace detail");

        then(executionLogRepository).should(times(1)).save(any(ExecutionLog.class));
    }

    @Test
    void createChildLog_withContext_shouldExtractIntegrationId() {
        given(executionLogRepository.save(any(ExecutionLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        ExecutionLog result = logService.createChildLog(
                "parent-1", "exec-1", "Class", "method",
                LogLevel.WARN, "warn msg", "detail", "999");

        assertThat(result.getIntegrationId()).isEqualTo("999");
    }

    @Test
    void createChildLog_whenRepositoryThrows_shouldReturnNull() {
        given(executionLogRepository.save(any(ExecutionLog.class))).willThrow(new RuntimeException("DB error"));

        ExecutionLog result = logService.createChildLog(
                "parent-1", "exec-1", "Class", "method",
                LogLevel.INFO, "msg", "detail");

        assertThat(result).isNull();
    }

    @Test
    void updateParentLog_whenFound_shouldUpdateFields() {
        ExecutionLog existingLog = ExecutionLog.builder()
                .id("log-id-1")
                .logLevel(LogLevel.INFO)
                .message("Original message")
                .durationMs(null)
                .build();

        given(executionLogRepository.findById("log-id-1")).willReturn(Optional.of(existingLog));
        given(executionLogRepository.save(any(ExecutionLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        logService.updateParentLog("log-id-1", LogLevel.ERROR, "Updated error message", 5000L);

        then(executionLogRepository).should(times(1)).findById("log-id-1");
        then(executionLogRepository).should(times(1)).save(any(ExecutionLog.class));

        assertThat(existingLog.getLogLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(existingLog.getMessage()).isEqualTo("Updated error message");
        assertThat(existingLog.getDurationMs()).isEqualTo(5000L);
    }

    @Test
    void updateParentLog_whenNotFound_shouldDoNothing() {
        given(executionLogRepository.findById("nonexistent")).willReturn(Optional.empty());

        logService.updateParentLog("nonexistent", LogLevel.ERROR, "Updated", 1000L);

        then(executionLogRepository).should(times(1)).findById("nonexistent");
        then(executionLogRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void updateParentLog_whenRepositoryThrows_shouldSwallowException() {
        ExecutionLog existingLog = ExecutionLog.builder().id("log-id-1").build();
        given(executionLogRepository.findById("log-id-1")).willReturn(Optional.of(existingLog));
        given(executionLogRepository.save(any(ExecutionLog.class))).willThrow(new RuntimeException("DB error"));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> logService.updateParentLog("log-id-1", LogLevel.ERROR, "msg", 100L));
    }
}
