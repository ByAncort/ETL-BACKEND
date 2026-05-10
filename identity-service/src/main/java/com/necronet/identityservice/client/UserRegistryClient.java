package com.necronet.identityservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistryClient {

    private final RestTemplate restTemplate;

    @Value("${user-registry-ms.url:http://localhost:9090}")
    private String userRegistryMsUrl;

    public Set<String> getUserRoles(String username, String token) {
        try {
            String baseUrl = userRegistryMsUrl.endsWith("/") 
                ? userRegistryMsUrl.substring(0, userRegistryMsUrl.length() - 1) 
                : userRegistryMsUrl;
            String url = baseUrl + "/api/users/username/" + username;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<UserRegistryResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, UserRegistryResponse.class);

            if (response.getBody() != null && response.getBody().getRoles() != null && !response.getBody().getRoles().isEmpty()) {
                log.debug("Roles obtenidos para '{}': {}", username, response.getBody().getRoles());
                return response.getBody().getRoles();
            }

            log.warn("No se encontraron roles para el usuario '{}'", username);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Usuario '{}' no encontrado en user-registry-ms", username);
        } catch (Exception e) {
            log.error("Error al obtener roles para '{}': {}", username, e.getMessage());
        }
        return Collections.emptySet();
    }

    public Set<String> getUserRoles(String username) {
        return getUserRoles(username, null);
    }

    public String getPrimaryRole(String username, String token) {
        return getUserRoles(username, token)
                .stream()
                .findFirst()
                .orElse("ROLE_USER");
    }

    public String getPrimaryRole(String username) {
        return getPrimaryRole(username, null);
    }
}