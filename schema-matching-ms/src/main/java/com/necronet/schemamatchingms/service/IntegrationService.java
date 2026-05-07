package com.necronet.schemamatchingms.service;

import com.necronet.schemamatchingms.dto.ConnectionResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IntegrationService {

    @Value("${external.api.base-url:http://localhost:8082}")
    private String baseUrl;

    @Value("${external.api.integrations.path:/api/integrations/connections/}")
    private String integrationsPath;

    private final RestTemplate restTemplate;

    public IntegrationService() {
        this.restTemplate = new RestTemplate();
    }

    public ConnectionResponseDTO getConnection(Long connectionId) {
        String url = baseUrl + integrationsPath + connectionId;
        return restTemplate.getForObject(url, ConnectionResponseDTO.class);
    }
}