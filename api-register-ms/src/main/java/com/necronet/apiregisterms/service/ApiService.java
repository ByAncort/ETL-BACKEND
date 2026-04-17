package com.necronet.apiregisterms.service;

import com.necronet.apiregisterms.dto.ApiRegisterRequest;
import com.necronet.apiregisterms.dto.ApiResponse;
import com.necronet.apiregisterms.dto.ApiUpdateRequest;
import com.necronet.apiregisterms.entity.*;
import com.necronet.apiregisterms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApiService {

    private final ApisRepository apisRepository;
    private final MethodRepository methodRepository;
    private final HeaderRepository headerRepository;
    private final AuthConfigRepository authConfigRepository;

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

            AuthConfig authConfig = AuthConfig.builder()
                    .api(savedApi)
                    .authType(request.getAuthType())
                    .header(header)
                    .credentialValue(request.getAuthValue())
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
                .authValue(api.getAuthConfig() != null ? api.getAuthConfig().getCredentialValue() : null)
                .build();
    }

    public String getAuthValue(Long apiId) {
        Apis api = apisRepository.findById(apiId)
                .orElseThrow(() -> new RuntimeException("API not found: " + apiId));

        if (api.getAuthApi() == null || api.getAuthApi().getAuthConfig() == null) {
            return null;
        }

        return api.getAuthApi().getAuthConfig().getCredentialValue();
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
            authValue = authApi.getAuthConfig().getCredentialValue();
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
            
            if (request.getAuthValue() != null) authConfig.setCredentialValue(request.getAuthValue());
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
            
            if (request.getAuthValue() != null) authConfig.setCredentialValue(request.getAuthValue());
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

            AuthConfig authConfig = AuthConfig.builder()
                    .api(savedAuthApi)
                    .authType(request.getAuthType())
                    .header(header)
                    .credentialValue(request.getAuthValue())
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