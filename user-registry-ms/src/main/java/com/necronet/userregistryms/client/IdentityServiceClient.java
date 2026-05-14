package com.necronet.userregistryms.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityServiceClient {

    private final RestTemplate restTemplate;
    @Value("${identity.service.url}")
    private String IDENTITY_SERVICE_URL;
    //private static final String IDENTITY_SERVICE_URL = "http://localhost:9898/api/v1/auth";

    public AuthResponse registerUserInIdentityService(String username, String email, String password) {
        try {
            String url = IDENTITY_SERVICE_URL + "/register";
            RegisterRequest request = new RegisterRequest(username, email, password, false);

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

    public void enableUser(String username) {
        try {
            String url = IDENTITY_SERVICE_URL + "/enable/" + username;

            // Para GET no necesitas body ni headers especiales
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            log.info("User enabled in identity-service. Response status: {}", response.getStatusCode());
            log.info("User {} enabled successfully", username);

        } catch (Exception e) {
            log.error("Error enabling user in identity-service: {}", e.getMessage());
            throw new RuntimeException("Failed to enable user in identity service", e);
        }
    }

    public void updatePassword(String username, String newPassword) {
        try {
            String url = IDENTITY_SERVICE_URL + "/update-password";

            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("newPassword", newPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            log.info("User {} password updated successfully in identity-service", username);
        } catch (Exception e) {
            log.error("Error updating password in identity-service: {}", e.getMessage());
            throw new RuntimeException("Failed to update password in identity service", e);
        }
    }
}