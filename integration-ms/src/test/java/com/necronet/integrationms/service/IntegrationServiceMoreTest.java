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
import org.springframework.http.HttpStatus;
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
class IntegrationServiceMoreTest {

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
    void createIntegration_withNullApiA_shouldNotThrowNPE() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA(null);
        request.setApiB("api-valid-b");
        request.setDescription("null apiA test");

        ResponseEntity<String> ok = ResponseEntity.ok("OK");
        given(restTemplate.getForEntity(anyString(), eq(String.class))).willReturn(ok);
        given(restTemplate.postForEntity(anyString(), isNull(), eq(String.class))).willReturn(ok);

        Integration saved = new Integration();
        saved.setId(10L);
        saved.setApiA(null);
        saved.setApiB("api-valid-b");
        saved.setDescription("null apiA test");
        saved.setStatus(IntegrationStatus.ACTIVE);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        given(integrationRepository.save(any(Integration.class))).willReturn(saved);

        IntegrationResponse response = integrationService.createIntegration(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getApiA()).isNull();
        assertThat(response.getApiB()).isEqualTo("api-valid-b");
        assertThat(response.getDescription()).isEqualTo("null apiA test");
    }

    @Test
    void createIntegration_withNullApiB_shouldNotThrowNPE() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("api-valid-a");
        request.setApiB(null);
        request.setDescription("null apiB test");

        ResponseEntity<String> ok = ResponseEntity.ok("OK");
        given(restTemplate.getForEntity(anyString(), eq(String.class))).willReturn(ok);
        given(restTemplate.postForEntity(anyString(), isNull(), eq(String.class))).willReturn(ok);

        Integration saved = new Integration();
        saved.setId(11L);
        saved.setApiA("api-valid-a");
        saved.setApiB(null);
        saved.setDescription("null apiB test");
        saved.setStatus(IntegrationStatus.ACTIVE);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        given(integrationRepository.save(any(Integration.class))).willReturn(saved);

        IntegrationResponse response = integrationService.createIntegration(request);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getApiA()).isEqualTo("api-valid-a");
        assertThat(response.getApiB()).isNull();
        assertThat(response.getDescription()).isEqualTo("null apiB test");
    }

    @Test
    void createIntegration_whenBothApiNull_shouldNotThrowNPE() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA(null);
        request.setApiB(null);
        request.setDescription("both null test");

        ResponseEntity<String> ok = ResponseEntity.ok("OK");
        given(restTemplate.getForEntity(anyString(), eq(String.class))).willReturn(ok);
        given(restTemplate.postForEntity(anyString(), isNull(), eq(String.class))).willReturn(ok);

        Integration saved = new Integration();
        saved.setId(12L);
        saved.setApiA(null);
        saved.setApiB(null);
        saved.setDescription("both null test");
        saved.setStatus(IntegrationStatus.ACTIVE);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        given(integrationRepository.save(any(Integration.class))).willReturn(saved);

        IntegrationResponse response = integrationService.createIntegration(request);

        assertThat(response.getId()).isEqualTo(12L);
        assertThat(response.getApiA()).isNull();
        assertThat(response.getApiB()).isNull();
    }

    @Test
    void createIntegration_whenSchemaMatchinIAFails_shouldThrowButSaveAlreadyHappened() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("api-valid-a");
        request.setApiB("api-valid-b");
        request.setDescription("schema fail");

        ResponseEntity<String> ok = ResponseEntity.ok("OK");
        given(restTemplate.getForEntity(anyString(), eq(String.class))).willReturn(ok);

        Integration saved = new Integration();
        saved.setId(20L);
        saved.setApiA("api-valid-a");
        saved.setApiB("api-valid-b");
        saved.setDescription("schema fail");
        given(integrationRepository.save(any(Integration.class))).willReturn(saved);

        given(restTemplate.postForEntity(contains("/run-matching/"), isNull(), eq(String.class)))
                .willThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> integrationService.createIntegration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid INTEGRATION ID");

        then(integrationRepository).should().save(any(Integration.class));
    }

    @Test
    void getAllIntegrations_whenAllDeleted_shouldReturnEmptyList() {
        given(integrationRepository.findByStatusNot(IntegrationStatus.DELETED))
                .willReturn(List.of());

        List<IntegrationResponse> responses = integrationService.getAllIntegrations();

        assertThat(responses).isEmpty();
    }

    @Test
    void getAllIntegrationsIncludingDeleted_whenRepositoryEmpty_shouldReturnEmptyList() {
        given(integrationRepository.findAll()).willReturn(List.of());

        List<IntegrationResponse> responses = integrationService.getAllIntegrationsIncludingDeleted();

        assertThat(responses).isEmpty();
    }

    @Test
    void getIntegrationById_withNullApiB_shouldHandleGracefully() {
        Integration integration = new Integration();
        integration.setId(30L);
        integration.setApiA("api-a");
        integration.setApiB(null);
        integration.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.findById(30L)).willReturn(Optional.of(integration));

        IntegrationResponse response = integrationService.getIntegrationById(30L);

        assertThat(response.getId()).isEqualTo(30L);
        assertThat(response.getApiA()).isEqualTo("api-a");
        assertThat(response.getApiB()).isNull();
    }

    @Test
    void getIntegrationById_withAllNullFields_shouldReturnResponseWithNulls() {
        Integration integration = new Integration();
        integration.setApiA(null);
        integration.setApiB(null);
        integration.setDescription(null);
        integration.setStatus(null);
        integration.setCreatedAt(null);
        integration.setUpdatedAt(null);

        given(integrationRepository.findById(40L)).willReturn(Optional.of(integration));

        IntegrationResponse response = integrationService.getIntegrationById(40L);

        assertThat(response.getId()).isNull();
        assertThat(response.getApiA()).isNull();
        assertThat(response.getApiB()).isNull();
        assertThat(response.getDescription()).isNull();
        assertThat(response.getStatus()).isNull();
        assertThat(response.getCreatedAt()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
    }

    @Test
    void runMatching_whenMatcherReturnsError_shouldThrow() {
        Integration integration = new Integration();
        integration.setId(50L);
        integration.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.findById(50L)).willReturn(Optional.of(integration));

        ResponseEntity<Map> errorResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        given(restTemplate.postForEntity(anyString(), isNull(), eq(Map.class))).willReturn(errorResponse);

        assertThatThrownBy(() -> integrationService.runMatching(50L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to run matcher");
    }

    @Test
    void runMatching_whenResponseBodyIsNull_shouldReturnNull() {
        Integration integration = new Integration();
        integration.setId(60L);
        integration.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.findById(60L)).willReturn(Optional.of(integration));

        ResponseEntity<Map> nullBodyResponse = ResponseEntity.ok(null);
        given(restTemplate.postForEntity(anyString(), isNull(), eq(Map.class))).willReturn(nullBodyResponse);

        Object result = integrationService.runMatching(60L);

        assertThat(result).isNull();
    }

    @Test
    void runMatching_whenResponseBodyIsNonMap_shouldReturnNonMap() {
        Integration integration = new Integration();
        integration.setId(70L);
        integration.setStatus(IntegrationStatus.ACTIVE);

        given(integrationRepository.findById(70L)).willReturn(Optional.of(integration));

        ResponseEntity nonMapResponse = ResponseEntity.ok("plain string result");
        given(restTemplate.postForEntity(anyString(), isNull(), eq(Map.class))).willReturn(nonMapResponse);

        Object result = integrationService.runMatching(70L);

        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("plain string result");
    }

    @Test
    void updateIntegrationStatus_fromDeletedToActive_shouldSucceed() {
        Integration integration = new Integration();
        integration.setId(80L);
        integration.setStatus(IntegrationStatus.DELETED);

        given(integrationRepository.findById(80L)).willReturn(Optional.of(integration));

        Integration updated = new Integration();
        updated.setId(80L);
        updated.setStatus(IntegrationStatus.ACTIVE);
        given(integrationRepository.save(any(Integration.class))).willReturn(updated);

        IntegrationResponse response = integrationService.updateIntegrationStatus(80L, IntegrationStatus.ACTIVE);

        assertThat(response.getStatus()).isEqualTo(IntegrationStatus.ACTIVE);
    }

    @Test
    void validateApiId_whenRegisterReturnsNon2xx_shouldThrow() {
        IntegrationRequest request = new IntegrationRequest();
        request.setApiA("api-non2xx");
        request.setApiB("api-valid-b");

        given(restTemplate.getForEntity(contains("api-non2xx"), eq(String.class)))
                .willReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        assertThatThrownBy(() -> integrationService.createIntegration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid API ID");

        then(integrationRepository).should(never()).save(any());
    }

    @Test
    void deleteIntegration_whenAlreadyDeleted_shouldNotThrow() {
        Integration integration = new Integration();
        integration.setId(90L);
        integration.setStatus(IntegrationStatus.DELETED);

        given(integrationRepository.findById(90L)).willReturn(Optional.of(integration));
        given(integrationRepository.save(any(Integration.class))).willAnswer(inv -> inv.getArgument(0));

        integrationService.deleteIntegration(90L);

        assertThat(integration.getStatus()).isEqualTo(IntegrationStatus.DELETED);
        then(integrationRepository).should().save(integration);
    }
}
