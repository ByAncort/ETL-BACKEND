package com.necronet.apiregisterms.service;

import com.necronet.apiregisterms.dto.ApiRegisterRequest;
import com.necronet.apiregisterms.dto.ApiResponse;
import com.necronet.apiregisterms.dto.ApiUpdateRequest;
import com.necronet.apiregisterms.dto.TestRequest;
import com.necronet.apiregisterms.dto.TestResponse;
import com.necronet.apiregisterms.entity.*;
import com.necronet.apiregisterms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;


import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiService {

    private final ApisRepository apisRepository;
    private final MethodRepository methodRepository;
    private final HeaderRepository headerRepository;
    private final AuthConfigRepository authConfigRepository;
    private final AuthCredentialRepository authCredentialRepository;
    private final WebClient webClient;

    @Transactional
    public Apis registerApi(ApiRegisterRequest request) {
        Apis authApi = null;
        if (request.getApiAuth() != null) {
            authApi = createApiEntity(request.getApiAuth());
        }

        Method httpMethod = methodRepository.findByName(request.getMethod().toUpperCase())
                .orElseGet(() -> {
                    Method newMethod = Method.builder()
                            .name(request.getMethod().toUpperCase())
                            .build();
                    return methodRepository.save(newMethod);
                });

        ApiEndpoint api = ApiEndpoint.builder()
                .method(httpMethod)
                .url(request.getUrl())
                .description(request.getDescription())
                .pathParams(request.getPathParams())
                .queryParams(request.getQueryParams())
                .body(request.getBody())
                .createdAt(LocalDateTime.now())
                .authApi(authApi)
                .build();

        Apis savedApi = apisRepository.save(api);

        if (request.getAuthType() != null && request.getAuthType() != AuthType.NONE) {
            Header header = headerRepository.findByValue(request.getAuthHeader() != null
                    ? request.getAuthHeader() 
                    : "Authorization")
                    .orElseGet(() -> headerRepository.save(Header.builder()
                            .value(request.getAuthHeader() != null ? request.getAuthHeader() : "Authorization")
                            .build()));

            AuthCredential authCredential = AuthCredential.builder()
                    .credentialValue(request.getAuthValue())
                    .build();
            authCredentialRepository.save(authCredential);

            AuthConfig authConfig = AuthConfig.builder()
                    .api(savedApi)
                    .authType(request.getAuthType())
                    .header(header)
                    .authCredential(authCredential)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .tokenEndpoint(request.getTokenEndpoint())
                    .createdAt(LocalDateTime.now())
                    .build();

            authConfigRepository.save(authConfig);
        }

        return savedApi;
    }

    public ApiResponse toResponse(Apis api) {
        Long authApiId = null;
        String authApiUrl = null;
        AuthType authType = null;
        String authHeader = null;

        if (api.getAuthApi() != null) {
            authApiId = api.getAuthApi().getId();
            authApiUrl = api.getAuthApi().getUrl();
        }

        if (api.getAuthConfig() != null) {
            authType = api.getAuthConfig().getAuthType();
            authHeader = api.getAuthConfig().getHeader() != null 
                    ? api.getAuthConfig().getHeader().getValue()
                    : null;
        }

        return ApiResponse.builder()
                .id(api.getId())
                .method(api.getMethod() != null ? api.getMethod().getName() : null)
                .url(api.getUrl())
                .description(api.getDescription())
                .pathParams(api instanceof ApiEndpoint ? ((ApiEndpoint) api).getPathParams() : null)
                .queryParams(api instanceof ApiEndpoint ? ((ApiEndpoint) api).getQueryParams() : null)
                .body(api instanceof ApiEndpoint ? ((ApiEndpoint) api).getBody() : null)
                .createdAt(api.getCreatedAt())
                .authType(authType)
                .authHeader(authHeader)
                .authApiId(authApiId)
                .authApiUrl(authApiUrl)
                .authValue(api.getAuthConfig() != null && api.getAuthConfig().getAuthCredential() != null
                        ? api.getAuthConfig().getAuthCredential().getCredentialValue() : null)
                .build();
    }

    public String getAuthValue(Long apiId) {
        Apis api = apisRepository.findById(apiId)
                .orElseThrow(() -> new RuntimeException("API not found: " + apiId));

        if (api.getAuthApi() == null || api.getAuthApi().getAuthConfig() == null) {
            return null;
        }

        return api.getAuthApi().getAuthConfig().getAuthCredential() != null
                ? api.getAuthApi().getAuthConfig().getAuthCredential().getCredentialValue() : null;
    }

    public ApiResponse getAuthApiResponse(Long apiId) {
        Apis api = apisRepository.findById(apiId)
                .orElseThrow(() -> new RuntimeException("API not found: " + apiId));

        if (api.getAuthApi() == null) {
            return null;
        }

        Apis authApi = api.getAuthApi();
        AuthType authType = null;
        String authHeader = null;
        String authValue = null;

        if (authApi.getAuthConfig() != null) {
            authType = authApi.getAuthConfig().getAuthType();
            authHeader = authApi.getAuthConfig().getHeader() != null 
                    ? authApi.getAuthConfig().getHeader().getValue()
                    : null;
            authValue = authApi.getAuthConfig().getAuthCredential() != null
                    ? authApi.getAuthConfig().getAuthCredential().getCredentialValue() : null;
        }

        return ApiResponse.builder()
                .method(authApi.getMethod() != null ? authApi.getMethod().getName() : null)
                .url(authApi.getUrl())
                .description(authApi.getDescription())
                .pathParams(authApi instanceof ApiEndpoint ? ((ApiEndpoint) authApi).getPathParams() : null)
                .queryParams(authApi instanceof ApiEndpoint ? ((ApiEndpoint) authApi).getQueryParams() : null)
                .body(authApi instanceof ApiEndpoint ? ((ApiEndpoint) authApi).getBody() : null)
                .authType(authType)
                .authHeader(authHeader)
                .authValue(authValue)
                .build();
    }
    public List<Apis> getListApis(){
        return apisRepository.findAll();
    }

    public TestResponse testApi(Long apiId, TestRequest request) {
        Apis api = apisRepository.findById(apiId).orElse(null);
        if (api == null) {
            return TestResponse.builder()
                    .statusCode(404)
                    .error("API not found")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        String methodName = api.getMethod() != null ? api.getMethod().getName() : "GET";
        String baseUrl = api.getUrl();
        String pathParams = request != null && request.getPathParams() != null ? request.getPathParams() : "";
        String queryParams = request != null && request.getQueryParams() != null ? request.getQueryParams() : "";
        String body = request != null && request.getBody() != null ? request.getBody() : null;

        if (api instanceof ApiEndpoint endpoint) {
            if (pathParams.isEmpty() && endpoint.getPathParams() != null) pathParams = endpoint.getPathParams();
            if (queryParams.isEmpty() && endpoint.getQueryParams() != null) queryParams = endpoint.getQueryParams();
            if (body == null && endpoint.getBody() != null) body = endpoint.getBody();
        }

        Map<String, String> authHeaders = resolveAuthHeaders(api.getAuthConfig());
        if (authHeaders.isEmpty() && api.getAuthApi() != null) {
            authHeaders = resolveAuthHeaders(api.getAuthApi().getAuthConfig());
        }

        String fullUrl = baseUrl + pathParams + queryParams;
        org.springframework.http.HttpMethod httpMethod = org.springframework.http.HttpMethod.valueOf(methodName.toUpperCase());
        long startTime = System.currentTimeMillis();

        try {
            WebClient.RequestHeadersSpec<?> requestSpec = buildRequest(webClient, httpMethod, fullUrl, authHeaders, body, methodName);

            // ✅ Todo en una sola cadena reactiva — un único .block()
            TestResponse response = requestSpec
                    .exchangeToMono(clientResponse -> {
                        int statusCode = clientResponse.statusCode().value();

                        Map<String, String> responseHeaders = new HashMap<>();
                        clientResponse.headers().asHttpHeaders()
                                .forEach((key, values) -> {
                                    if (!values.isEmpty()) responseHeaders.put(key, values.get(0));
                                });

                        // Leer body dentro del mismo contexto reactivo
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(responseBody -> TestResponse.builder()
                                        .statusCode(statusCode)
                                        .body(responseBody.isBlank() ? null : responseBody)
                                        .headers(responseHeaders)
                                        .responseTimeMs(System.currentTimeMillis() - startTime)
                                        .timestamp(LocalDateTime.now())
                                        .build());
                    })
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return response != null ? response : TestResponse.builder()
                    .statusCode(500)
                    .error("No response received")
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            String errorMessage = e.getMessage();
            int statusCode = 500;

            String msg = errorMessage != null ? errorMessage.toLowerCase() : "";
            if (msg.contains("404") || msg.contains("not found")) statusCode = 404;
            else if (msg.contains("401") || msg.contains("unauthorized")) statusCode = 401;
            else if (msg.contains("403") || msg.contains("forbidden")) statusCode = 403;
            else if (msg.contains("timeout")) statusCode = 408;

            return TestResponse.builder()
                    .statusCode(statusCode)
                    .error(errorMessage)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    private WebClient.RequestHeadersSpec<?> buildRequest(
            WebClient webClient,
            org.springframework.http.HttpMethod method,
            String url,
            Map<String, String> authHeaders,
            String body,
            String methodName) {

        boolean supportsBody = body != null && !body.isBlank() &&
                List.of("POST", "PUT", "PATCH", "GET").contains(methodName.toUpperCase());

        if (supportsBody) {
            WebClient.RequestBodySpec spec = webClient.method(method).uri(url);

            for (Map.Entry<String, String> e : authHeaders.entrySet()) {
                spec = spec.header(e.getKey(), e.getValue());
            }

            boolean isJson = body.trim().startsWith("{") || body.trim().startsWith("[");
            return spec
                    .contentType(isJson ? MediaType.APPLICATION_JSON : MediaType.TEXT_PLAIN)
                    .bodyValue(body);
        } else {
            WebClient.RequestHeadersSpec<?> spec = webClient.method(method).uri(url);

            for (Map.Entry<String, String> e : authHeaders.entrySet()) {
                spec = spec.header(e.getKey(), e.getValue());
            }

            return spec;
        }
    }

    private Map<String, String> resolveAuthHeaders(AuthConfig authConfig) {
        Map<String, String> headers = new HashMap<>();

        if (authConfig == null || authConfig.getAuthType() == null
                || authConfig.getAuthType() == AuthType.NONE) {
            return headers;
        }

        String headerName = authConfig.getHeader() != null
                ? authConfig.getHeader().getValue()
                : "Authorization";

        String credentialValue = authConfig.getAuthCredential() != null
                ? authConfig.getAuthCredential().getCredentialValue()
                : null;

        if (credentialValue == null || credentialValue.isBlank()) return headers;

        switch (authConfig.getAuthType()) {
            case BEARER -> headers.put(headerName, "Bearer " + credentialValue);
            case BASIC -> {
                String encoded = credentialValue.contains(":")
                        ? Base64.getEncoder().encodeToString(credentialValue.getBytes(StandardCharsets.UTF_8))
                        : credentialValue;
                headers.put(headerName, "Basic " + encoded);
            }
            case API_KEY -> headers.put(headerName, credentialValue);
            case OAUTH2  -> headers.put(headerName, "Bearer " + credentialValue);
        }

        return headers;
    }
// --- Extracted helper ---

    /*private Map<String, String> resolveAuthHeaders(AuthConfig authConfig) {
        Map<String, String> headers = new HashMap<>();

        if (authConfig == null || authConfig.getAuthType() == null
                || authConfig.getAuthType() == AuthType.NONE) {
            return headers;
        }

        String headerName = authConfig.getHeader() != null
                ? authConfig.getHeader().getValue()
                : "Authorization";

        String credentialValue = authConfig.getAuthCredential() != null
                ? authConfig.getAuthCredential().getCredentialValue()
                : null;

        if (credentialValue == null || credentialValue.isBlank()) return headers;

        switch (authConfig.getAuthType()) {
            case BEARER -> headers.put(headerName, "Bearer " + credentialValue);

            case BASIC -> {
                // credentialValue can be a raw "user:pass" or already Base64-encoded
                String encoded = credentialValue.contains(":")
                        ? Base64.getEncoder().encodeToString(credentialValue.getBytes(StandardCharsets.UTF_8))
                        : credentialValue;
                headers.put(headerName, "Basic " + encoded);
            }

            case API_KEY ->
                // headerName is whatever the API expects (e.g. "X-Api-Key")
                    headers.put(headerName, credentialValue);

            case OAUTH2 ->
                // Token should already be resolved/refreshed before reaching here
                    headers.put(headerName, "Bearer " + credentialValue);
        }

        return headers;
    }*/

    public Apis executeRequest(Long apiId) {
        Apis api = apisRepository.findById(apiId)
                .orElseThrow(() -> new RuntimeException("API not found: " + apiId));

        String fullUrl = buildFullUrl(api);
        String methodName = api.getMethod().getName();

        return api;
    }

    @Transactional
    public Apis updateApi(Long apiId, ApiUpdateRequest request) {
        Apis api = apisRepository.findById(apiId).orElse(null);
        if (api == null) return null;

        if (request.getMethod() != null) {
            Method httpMethod = methodRepository.findByName(request.getMethod().toUpperCase())
                    .orElseGet(() -> methodRepository.save(Method.builder().name(request.getMethod().toUpperCase()).build()));
            api.setMethod(httpMethod);
        }
        if (request.getUrl() != null) api.setUrl(request.getUrl());
        if (request.getDescription() != null) api.setDescription(request.getDescription());
        if (api instanceof ApiEndpoint) {
            ApiEndpoint endpoint = (ApiEndpoint) api;
            if (request.getPathParams() != null) endpoint.setPathParams(request.getPathParams());
            if (request.getQueryParams() != null) endpoint.setQueryParams(request.getQueryParams());
            if (request.getBody() != null) endpoint.setBody(request.getBody());
        }

        if (request.getAuthType() != null && request.getAuthType() != AuthType.NONE) {
            AuthConfig authConfig = api.getAuthConfig();
            if (authConfig == null) {
                authConfig = AuthConfig.builder().api(api).createdAt(LocalDateTime.now()).build();
                api.setAuthConfig(authConfig);
            }
            authConfig.setAuthType(request.getAuthType());
            
            if (request.getAuthHeader() != null) {
                Header header = headerRepository.findByValue(request.getAuthHeader())
                        .orElseGet(() -> headerRepository.save(Header.builder().value(request.getAuthHeader()).build()));
                authConfig.setHeader(header);
            }
            
            if (request.getAuthValue() != null) {
                AuthCredential authCredential = authConfig.getAuthCredential();
                if (authCredential == null) {
                    authCredential = AuthCredential.builder().build();
                    authConfig.setAuthCredential(authCredential);
                }
                authCredential.setCredentialValue(request.getAuthValue());
                authCredentialRepository.save(authCredential);
            }
            if (request.getUsername() != null) authConfig.setUsername(request.getUsername());
            if (request.getPassword() != null) authConfig.setPassword(request.getPassword());
            if (request.getTokenEndpoint() != null) authConfig.setTokenEndpoint(request.getTokenEndpoint());
            
            authConfigRepository.save(authConfig);
        }

        return apisRepository.save(api);
    }

    @Transactional
    public ApiResponse updateAuthApi(Long apiId, ApiUpdateRequest request) {
        Apis api = apisRepository.findById(apiId).orElse(null);
        if (api == null || api.getAuthApi() == null) return null;

        Apis authApi = api.getAuthApi();

        if (request.getMethod() != null) {
            Method httpMethod = methodRepository.findByName(request.getMethod().toUpperCase())
                    .orElseGet(() -> methodRepository.save(Method.builder().name(request.getMethod().toUpperCase()).build()));
            authApi.setMethod(httpMethod);
        }
        if (request.getUrl() != null) authApi.setUrl(request.getUrl());
        if (request.getDescription() != null) authApi.setDescription(request.getDescription());
        if (authApi instanceof ApiEndpoint) {
            ApiEndpoint endpoint = (ApiEndpoint) authApi;
            if (request.getPathParams() != null) endpoint.setPathParams(request.getPathParams());
            if (request.getQueryParams() != null) endpoint.setQueryParams(request.getQueryParams());
            if (request.getBody() != null) endpoint.setBody(request.getBody());
        }

        if (request.getAuthType() != null && request.getAuthType() != AuthType.NONE) {
            AuthConfig authConfig = authApi.getAuthConfig();
            if (authConfig == null) {
                authConfig = AuthConfig.builder().api(authApi).createdAt(LocalDateTime.now()).build();
                authApi.setAuthConfig(authConfig);
            }
            authConfig.setAuthType(request.getAuthType());
            
            if (request.getAuthHeader() != null) {
                Header header = headerRepository.findByValue(request.getAuthHeader())
                        .orElseGet(() -> headerRepository.save(Header.builder().value(request.getAuthHeader()).build()));
                authConfig.setHeader(header);
            }
            
            if (request.getAuthValue() != null) {
                AuthCredential authCredential = authConfig.getAuthCredential();
                if (authCredential == null) {
                    authCredential = AuthCredential.builder().build();
                    authConfig.setAuthCredential(authCredential);
                }
                authCredential.setCredentialValue(request.getAuthValue());
                authCredentialRepository.save(authCredential);
            }
            if (request.getUsername() != null) authConfig.setUsername(request.getUsername());
            if (request.getPassword() != null) authConfig.setPassword(request.getPassword());
            if (request.getTokenEndpoint() != null) authConfig.setTokenEndpoint(request.getTokenEndpoint());
            
            authConfigRepository.save(authConfig);
        }

        apisRepository.save(authApi);
        return getAuthApiResponse(apiId);
    }

    private String buildFullUrl(Apis api) {
        StringBuilder url = new StringBuilder(api.getUrl());
        
        if (api instanceof ApiEndpoint) {
            ApiEndpoint endpoint = (ApiEndpoint) api;
            if (endpoint.getPathParams() != null) {
                url.append(endpoint.getPathParams());
            }
            if (endpoint.getQueryParams() != null) {
                url.append(endpoint.getQueryParams());
            }
        }
        
        return url.toString();
    }

    private Apis createApiEntity(ApiRegisterRequest request) {
        Method httpMethod = methodRepository.findByName(request.getMethod().toUpperCase())
                .orElseGet(() -> {
                    Method newMethod = Method.builder()
                            .name(request.getMethod().toUpperCase())
                            .build();
                    return methodRepository.save(newMethod);
                });

        ApiEndpoint apiEntity = ApiEndpoint.builder()
                .method(httpMethod)
                .url(request.getUrl())
                .description(request.getDescription())
                .pathParams(request.getPathParams())
                .queryParams(request.getQueryParams())
                .body(request.getBody())
                .createdAt(LocalDateTime.now())
                .build();

        Apis savedAuthApi = apisRepository.save(apiEntity);

        if (request.getAuthType() != null && request.getAuthType() != AuthType.NONE) {
            Header header = headerRepository.findByValue(request.getAuthHeader() != null
                    ? request.getAuthHeader() 
                    : "Authorization")
                    .orElseGet(() -> headerRepository.save(Header.builder()
                            .value(request.getAuthHeader() != null ? request.getAuthHeader() : "Authorization")
                            .build()));

            AuthCredential authCredential = AuthCredential.builder()
                    .credentialValue(request.getAuthValue())
                    .build();
            authCredentialRepository.save(authCredential);

            AuthConfig authConfig = AuthConfig.builder()
                    .api(savedAuthApi)
                    .authType(request.getAuthType())
                    .header(header)
                    .authCredential(authCredential)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .tokenEndpoint(request.getTokenEndpoint())
                    .createdAt(LocalDateTime.now())
                    .build();

            authConfigRepository.save(authConfig);
        }

        return savedAuthApi;
    }
}