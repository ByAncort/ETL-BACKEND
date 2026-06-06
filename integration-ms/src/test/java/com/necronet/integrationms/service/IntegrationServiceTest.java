package com.necronet.integrationms.service;

import com.necronet.integrationms.dto.IntegrationRequest;
import com.necronet.integrationms.dto.IntegrationResponse;
import com.necronet.integrationms.entity.Integration;
import com.necronet.integrationms.entity.IntegrationStatus;
import com.necronet.integrationms.repository.IntegrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tema 5 — Integracion E2E.
 * Verifica el flujo de orquestacion de IntegrationService: validar ambas APIs
 * contra api-register-ms, persistir la integracion y disparar el matching en
 * el matcher. Las llamadas HTTP salientes (RestTemplate) se mockean.
 */
@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {

    @Mock
    private IntegrationRepository integrationRepository;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private IntegrationService integrationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(integrationService, "apiRegisterMsUrl", "http://api-register-ms");
        ReflectionTestUtils.setField(integrationService, "apiMatcherIA", "http://matcher-ms");
    }

    private IntegrationRequest request() {
        IntegrationRequest req = new IntegrationRequest();
        req.setApiA("10");
        req.setApiB("20");
        req.setDescription("flujo e2e");
        return req;
    }

    private Integration saved(Long id) {
        Integration integration = new Integration();
        integration.setId(id);
        integration.setApiA("10");
        integration.setApiB("20");
        integration.setDescription("flujo e2e");
        integration.setStatus(IntegrationStatus.ACTIVE);
        return integration;
    }

    @Test
    @DisplayName("createIntegration valida ambas APIs, guarda y dispara matching")
    void createIntegration_happyPath() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));
        when(integrationRepository.save(any(Integration.class))).thenReturn(saved(1L));
        when(restTemplate.postForEntity(anyString(), isNull(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("matching-started"));

        IntegrationResponse response = integrationService.createIntegration(request());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getApiA()).isEqualTo("10");
        // valida apiA y apiB → 2 GET
        verify(restTemplate, times(2)).getForEntity(anyString(), eq(String.class));
        verify(integrationRepository).save(any(Integration.class));
        // dispara matching → 1 POST al matcher
        verify(restTemplate).postForEntity(contains("/run-matching/1"), isNull(), eq(String.class));
    }

    @Test
    @DisplayName("createIntegration con API invalida lanza error y no persiste")
    void createIntegration_invalidApi() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body("missing"));

        assertThatThrownBy(() -> integrationService.createIntegration(request()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid API ID");

        verify(integrationRepository, never()).save(any());
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("runMatching reactiva una integracion DELETED y consulta al matcher")
    void runMatching_reactivatesDeleted() {
        Integration deleted = saved(5L);
        deleted.setStatus(IntegrationStatus.DELETED);
        when(integrationRepository.findById(5L)).thenReturn(Optional.of(deleted));
        when(integrationRepository.save(any(Integration.class))).thenReturn(deleted);
        when(restTemplate.postForEntity(anyString(), isNull(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("matches", 3)));

        Object result = integrationService.runMatching(5L);

        assertThat(deleted.getStatus()).isEqualTo(IntegrationStatus.ACTIVE);
        verify(integrationRepository).save(deleted);
        verify(restTemplate).postForEntity(contains("/run-matching/5"), isNull(), eq(Map.class));
        assertThat(result).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("runMatching con id inexistente lanza error")
    void runMatching_notFound() {
        when(integrationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> integrationService.runMatching(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("deleteIntegration marca status DELETED (borrado logico)")
    void deleteIntegration_softDelete() {
        Integration active = saved(7L);
        when(integrationRepository.findById(7L)).thenReturn(Optional.of(active));

        integrationService.deleteIntegration(7L);

        assertThat(active.getStatus()).isEqualTo(IntegrationStatus.DELETED);
        verify(integrationRepository).save(active);
    }

    @Test
    @DisplayName("getAllIntegrations excluye las DELETED")
    void getAllIntegrations_excludesDeleted() {
        when(integrationRepository.findByStatusNot(IntegrationStatus.DELETED))
                .thenReturn(java.util.List.of(saved(1L), saved(2L)));

        assertThat(integrationService.getAllIntegrations()).hasSize(2);
        verify(integrationRepository).findByStatusNot(IntegrationStatus.DELETED);
    }
}
