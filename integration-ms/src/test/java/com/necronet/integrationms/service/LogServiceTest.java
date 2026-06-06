package com.necronet.integrationms.service;

import com.necronet.integrationms.entity.ExecutionLog;
import com.necronet.integrationms.entity.LogLevel;
import com.necronet.integrationms.repository.ExecutionLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tema 4 — Procesos Async ETL ("proceso registrado").
 * Pruebas unitarias de LogService: verifican que la ejecucion del proceso
 * queda registrada (parent/child logs) y que un fallo al guardar el log NO
 * rompe el proceso que lo invoca.
 */
@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private ExecutionLogRepository logRepository;

    @InjectMocks
    private LogService logService;

    private void setServiceName() {
        ReflectionTestUtils.setField(logService, "serviceName", "integration-ms-test");
    }

    @Test
    @DisplayName("createParentLog registra un log INFO raiz con ids generados")
    void createParentLog_persistsRootInfoLog() {
        setServiceName();
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExecutionLog result = logService.createParentLog(
                "EtlOrchestrator", "runEtl", "Inicio ETL", "42");

        ArgumentCaptor<ExecutionLog> captor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).save(captor.capture());
        ExecutionLog saved = captor.getValue();

        assertThat(saved.getParentId()).isNull();
        assertThat(saved.getLogLevel()).isEqualTo(LogLevel.INFO);
        assertThat(saved.getMessage()).isEqualTo("Inicio ETL");
        assertThat(saved.getServiceName()).isEqualTo("integration-ms-test");
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getExecutionId()).isNotBlank();
        assertThat(saved.getIntegrationId()).isEqualTo("42");
        assertThat(saved.getTimestamp()).isNotNull();
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("createParentLog ignora contexto no numerico (integrationId queda null)")
    void createParentLog_ignoresNonNumericContext() {
        setServiceName();
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        logService.createParentLog("Clase", "metodo", "msg", "no-es-id", "tampoco");

        ArgumentCaptor<ExecutionLog> captor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getIntegrationId()).isNull();
    }

    @Test
    @DisplayName("createParentLog devuelve null si falla el guardado (no rompe el proceso)")
    void createParentLog_returnsNullOnFailure() {
        setServiceName();
        when(logRepository.save(any())).thenThrow(new RuntimeException("DB caida"));

        ExecutionLog result = logService.createParentLog("Clase", "metodo", "msg", "1");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("createChildLog registra un log hijo con parentId, executionId y nivel dados")
    void createChildLog_persistsChildWithParent() {
        setServiceName();
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExecutionLog child = logService.createChildLog(
                "parent-1", "exec-1", "EtlOrchestrator", "extract",
                LogLevel.ERROR, "fallo extract", "stacktrace", "7");

        ArgumentCaptor<ExecutionLog> captor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).save(captor.capture());
        ExecutionLog saved = captor.getValue();

        assertThat(saved.getParentId()).isEqualTo("parent-1");
        assertThat(saved.getExecutionId()).isEqualTo("exec-1");
        assertThat(saved.getLogLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(saved.getDetail()).isEqualTo("stacktrace");
        assertThat(saved.getIntegrationId()).isEqualTo("7");
        assertThat(child).isSameAs(saved);
    }

    @Test
    @DisplayName("updateParentLog actualiza nivel, mensaje y duracion cuando el log existe")
    void updateParentLog_updatesWhenPresent() {
        setServiceName();
        ExecutionLog existing = ExecutionLog.builder()
                .id("p-1")
                .executionId("e-1")
                .serviceName("integration-ms-test")
                .className("c").methodName("m")
                .logLevel(LogLevel.INFO)
                .message("en curso")
                .build();
        when(logRepository.findById("p-1")).thenReturn(Optional.of(existing));

        logService.updateParentLog("p-1", LogLevel.ERROR, "termino con error", 1500L);

        ArgumentCaptor<ExecutionLog> captor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).save(captor.capture());
        ExecutionLog saved = captor.getValue();

        assertThat(saved.getLogLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(saved.getMessage()).isEqualTo("termino con error");
        assertThat(saved.getDurationMs()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("updateParentLog no hace nada si el log no existe")
    void updateParentLog_noopWhenAbsent() {
        setServiceName();
        when(logRepository.findById("missing")).thenReturn(Optional.empty());

        logService.updateParentLog("missing", LogLevel.ERROR, "x", 100L);

        verify(logRepository, never()).save(any());
    }
}
