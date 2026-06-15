package com.necronet.integrationms.service;

import com.necronet.integrationms.dto.IntegrationRequest;
import com.necronet.integrationms.dto.IntegrationResponse;
import com.necronet.integrationms.entity.Integration;
import com.necronet.integrationms.entity.IntegrationStatus;
import com.necronet.integrationms.repository.IntegrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
        ReflectionTestUtils.setField(integrationService, "apiRegisterMsUrl", "http://test-register:8080");
        ReflectionTestUtils.setField(integrationService, "apiMatcherIA", "http://test-matcher:8000");
    }

    @Test
    void createIntegration_shouldReturnResponse() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("api-valid-1");
        request.setApiB("api-valid-2");
        request.setDescription("test integration");

        ResponseEntity<String> okResponse = ResponseEntity.ok("OK");
        given(restTemplate.getForEntity(anyString(), eq(String.class))).willReturn(okResponse);
        given(restTemplate.postForEntity(anyString(), isNull(), eq(String.class))).willReturn(okResponse);

        Integration saved = new Integration();
        saved.setId(1L);
        saved.setApiA("api-valid-1");
        saved.setApiB("api-valid-2");
        saved.setDescription("test integration");
        saved.setStatus(IntegrationStatus.ACTIVE);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());

        given(integrationRepository.save(any(Integration.class))).willReturn(saved);

        IntegrationResponse response = integrationService.createIntegration(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getApiA()).isEqualTo("api-valid-1");
        assertThat(response.getApiB()).isEqualTo("api-valid-2");
        assertThat(response.getDescription()).isEqualTo("test integration");
        assertThat(response.getStatus()).isEqualTo(IntegrationStatus.ACTIVE);
    }

    @Test
    void createIntegration_whenApiAInvalid_shouldThrow() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("invalid-api");
        request.setApiB("api-valid-2");

        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willThrow(new RuntimeException("Invalid API ID: invalid-api"));

        assertThatThrownBy(() -> integrationService.createIntegration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid API ID");

        then(integrationRepository).should(never()).save(any());
    }

    @Test
    void createIntegration_whenApiBInvalid_shouldThrow() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("api-valid-1");
        request.setApiB("invalid-api");

        ResponseEntity<String> okResponse = ResponseEntity.ok("OK");
        given(restTemplate.getForEntity(contains("api-valid-1"), eq(String.class))).willReturn(okResponse);
        given(restTemplate.getForEntity(contains("invalid-api"), eq(String.class)))
                .willThrow(new RuntimeException("Invalid API ID: invalid-api"));

        assertThatThrownBy(() -> integrationService.createIntegration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid API ID");
    }

    @Test
    void getAllIntegrations_shouldReturnNonDeletedOnly() {
        Integration active1 = new Integration();
        active1.setId(1L);
        active1.setApiA("api-a");
        active1.setApiB("api-b");
        active1.setStatus(IntegrationStatus.ACTIVE);

        Integration active2 = new Integration();
        active2.setId(2L);
        active2.setApiA("api-c");
        active2.setApiB("api-d");
        active2.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.findByStatusNot(IntegrationStatus.DELETED))
                .willReturn(List.of(active1, active2));

        List<IntegrationResponse> responses = integrationService.getAllIntegrations();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void getAllIntegrationsIncludingDeleted_shouldReturnAll() {
        Integration i1 = new Integration();
        i1.setId(1L);
        i1.setStatus(IntegrationStatus.ACTIVE);

        Integration i2 = new Integration();
        i2.setId(2L);
        i2.setStatus(IntegrationStatus.DELETED);

        given(integrationRepository.findAll()).willReturn(List.of(i1, i2));

        List<IntegrationResponse> responses = integrationService.getAllIntegrationsIncludingDeleted();

        assertThat(responses).hasSize(2);
    }

    @Test
    void getIntegrationById_whenFound_shouldReturnResponse() {
        Integration integration = new Integration();
        integration.setId(10L);
        integration.setApiA("api-a");
        integration.setApiB("api-b");
        integration.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.findById(10L)).willReturn(Optional.of(integration));

        IntegrationResponse response = integrationService.getIntegrationById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getApiA()).isEqualTo("api-a");
        assertThat(response.getApiB()).isEqualTo("api-b");
    }

    @Test
    void getIntegrationById_whenNotFound_shouldThrow() {
        given(integrationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> integrationService.getIntegrationById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Integration not found");
    }

    @Test
    void updateIntegration_shouldReturnUpdatedResponse() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("api-updated-a");
        request.setApiB("api-updated-b");
        request.setDescription("updated desc");

        ResponseEntity<String> okResponse = ResponseEntity.ok("OK");
        given(restTemplate.getForEntity(anyString(), eq(String.class))).willReturn(okResponse);

        Integration existing = new Integration();
        existing.setId(5L);
        existing.setApiA("api-old-a");
        existing.setApiB("api-old-b");

        given(integrationRepository.findById(5L)).willReturn(Optional.of(existing));

        Integration updated = new Integration();
        updated.setId(5L);
        updated.setApiA("api-updated-a");
        updated.setApiB("api-updated-b");
        updated.setDescription("updated desc");
        updated.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.save(any(Integration.class))).willReturn(updated);

        IntegrationResponse response = integrationService.updateIntegration(5L, request);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getApiA()).isEqualTo("api-updated-a");
        assertThat(response.getApiB()).isEqualTo("api-updated-b");
        assertThat(response.getDescription()).isEqualTo("updated desc");
    }

    @Test
    void updateIntegration_whenNotFound_shouldThrow() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("api-a");
        request.setApiB("api-b");

        ResponseEntity<String> okResponse = ResponseEntity.ok("OK");
        given(restTemplate.getForEntity(anyString(), eq(String.class))).willReturn(okResponse);
        given(integrationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> integrationService.updateIntegration(99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Integration not found");
    }

    @Test
    void deleteIntegration_shouldSoftDelete() {
        Integration integration = new Integration();
        integration.setId(7L);
        integration.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.findById(7L)).willReturn(Optional.of(integration));
        given(integrationRepository.save(any(Integration.class))).willAnswer(inv -> inv.getArgument(0));

        integrationService.deleteIntegration(7L);

        assertThat(integration.getStatus()).isEqualTo(IntegrationStatus.DELETED);
        then(integrationRepository).should().save(integration);
    }

    @Test
    void deleteIntegration_whenNotFound_shouldThrow() {
        given(integrationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> integrationService.deleteIntegration(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Integration not found");
    }

    @Test
    void runMatching_shouldReturnResult() {
        Integration integration = new Integration();
        integration.setId(15L);
        integration.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.findById(15L)).willReturn(Optional.of(integration));

        Map<String, Object> matcherResult = Map.of("match", true, "score", 0.85);
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(matcherResult);
        given(restTemplate.postForEntity(anyString(), isNull(), eq(Map.class))).willReturn(responseEntity);

        Object result = integrationService.runMatching(15L);

        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("match")).isEqualTo(true);
        assertThat(resultMap.get("score")).isEqualTo(0.85);
    }

    @Test
    void runMatching_whenNotFound_shouldThrow() {
        given(integrationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> integrationService.runMatching(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Integration not found");
    }

    @Test
    void runMatching_whenDeleted_shouldReactivateAndRun() {
        Integration integration = new Integration();
        integration.setId(20L);
        integration.setStatus(IntegrationStatus.DELETED);

        given(integrationRepository.findById(20L)).willReturn(Optional.of(integration));

        Map<String, Object> matcherResult = Map.of("match", true, "score", 0.92);
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(matcherResult);
        given(restTemplate.postForEntity(anyString(), isNull(), eq(Map.class))).willReturn(responseEntity);

        given(integrationRepository.save(any(Integration.class))).willAnswer(inv -> inv.getArgument(0));

        Object result = integrationService.runMatching(20L);

        assertThat(integration.getStatus()).isEqualTo(IntegrationStatus.ACTIVE);
        assertThat(((Map<String, Object>) result).get("score")).isEqualTo(0.92);
    }

    @Test
    void updateIntegrationStatus_shouldUpdateAndReturnResponse() {
        Integration integration = new Integration();
        integration.setId(30L);
        integration.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.findById(30L)).willReturn(Optional.of(integration));

        Integration updated = new Integration();
        updated.setId(30L);
        updated.setStatus(IntegrationStatus.DELETED);
        given(integrationRepository.save(any(Integration.class))).willReturn(updated);

        IntegrationResponse response = integrationService.updateIntegrationStatus(30L, IntegrationStatus.DELETED);

        assertThat(response.getStatus()).isEqualTo(IntegrationStatus.DELETED);
    }

    @Test
    void updateIntegrationStatus_whenNotFound_shouldThrow() {
        given(integrationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> integrationService.updateIntegrationStatus(99L, IntegrationStatus.ACTIVE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Integration not found");
    }
}
