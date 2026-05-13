package com.necronet.integrationms.service;

import com.necronet.integrationms.dto.IntegrationRequest;
import com.necronet.integrationms.dto.IntegrationResponse;
import com.necronet.integrationms.entity.Integration;
import com.necronet.integrationms.entity.IntegrationStatus;
import com.necronet.integrationms.repository.IntegrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationService {

    private final IntegrationRepository integrationRepository;
    private final RestTemplate restTemplate;

    @Value("${api.register.ms.url}")
    private String apiRegisterMsUrl;

    public IntegrationResponse createIntegration(IntegrationRequest request) {
        validateApiId(request.getApiA());
        validateApiId(request.getApiB());

        Integration integration = new Integration();
        integration.setApiA(request.getApiA());
        integration.setApiB(request.getApiB());
        integration.setDescription(request.getDescription());

        Integration savedIntegration = integrationRepository.save(integration);
        return mapToResponse(savedIntegration);
    }

    public List<IntegrationResponse> getAllIntegrations() {
        return integrationRepository.findByStatusNot(IntegrationStatus.DELETED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<IntegrationResponse> getAllIntegrationsIncludingDeleted() {
        return integrationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public IntegrationResponse getIntegrationById(Long id) {
        Integration integration = integrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration not found with id: " + id));
        return mapToResponse(integration);
    }

    public IntegrationResponse updateIntegration(Long id, IntegrationRequest request) {
        validateApiId(request.getApiA());
        validateApiId(request.getApiB());

        Integration integration = integrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration not found with id: " + id));

        integration.setApiA(request.getApiA());
        integration.setApiB(request.getApiB());
        integration.setDescription(request.getDescription());

        Integration updatedIntegration = integrationRepository.save(integration);
        return mapToResponse(updatedIntegration);
    }

    public void deleteIntegration(Long id) {
        Integration integration = integrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration not found with id: " + id));
        integration.setStatus(IntegrationStatus.DELETED);
        integrationRepository.save(integration);
    }

    private void validateApiId(String apiId) {
        try {
            String url = apiRegisterMsUrl + "/api-registry/" + apiId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Invalid API ID: " + apiId);
            }
        } catch (Exception e) {
            log.error("Error validating API ID: {}", apiId, e);
            throw new RuntimeException("Invalid API ID: " + apiId);
        }
    }

    public IntegrationResponse updateIntegrationStatus(Long id, IntegrationStatus status) {
        Integration integration = integrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration not found with id: " + id));
        integration.setStatus(status);
        Integration updated = integrationRepository.save(integration);
        return mapToResponse(updated);
    }

    private IntegrationResponse mapToResponse(Integration integration) {
        IntegrationResponse response = new IntegrationResponse();
        response.setId(integration.getId());
        response.setApiA(integration.getApiA());
        response.setApiB(integration.getApiB());
        response.setDescription(integration.getDescription());
        response.setStatus(integration.getStatus());
        response.setCreatedAt(integration.getCreatedAt());
        response.setUpdatedAt(integration.getUpdatedAt());
        return response;
    }
}
