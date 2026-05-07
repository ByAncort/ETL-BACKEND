package com.necronet.userregistryms.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityServiceClient {

    private final RestTemplate restTemplate;
    private static final String IDENTITY_SERVICE_URL = "http://localhost:9898/api/v1/auth";

    public AuthResponse registerUserInIdentityService(String username, String email, String password) {
        try {
            String url = IDENTITY_SERVICE_URL + "/register";
            RegisterRequest request = new RegisterRequest(username, email, password);

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AuthResponse.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("HTTP Error registering user: Status={}, Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to register user: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error registering user in identity-service: {}", e.getMessage());
            throw new RuntimeException("Failed to register user in identity service", e);
        }
    }

    public String generateToken(String username, String password) {
        try {
            String url = IDENTITY_SERVICE_URL + "/token";
            AuthRequest request = new AuthRequest(username, password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AuthRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AuthResponse.class
            );

            return response.getBody() != null ? response.getBody().getAccessToken() : null;
        } catch (Exception e) {
            log.error("Error generating token from identity-service: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            String url = IDENTITY_SERVICE_URL + "/validate?token=" + token;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token); // Si el endpoint requiere autenticación
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    AuthResponse.class
            );

            return response.getBody() != null && "Token is valid".equals(response.getBody().getMessage());
        } catch (Exception e) {
            log.error("Error validating token with identity-service: {}", e.getMessage());
            return false;
        }
    }
}