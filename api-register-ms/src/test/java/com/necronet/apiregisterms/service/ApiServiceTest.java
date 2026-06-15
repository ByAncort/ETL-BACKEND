package com.necronet.apiregisterms.service;

import com.necronet.apiregisterms.dto.ApiRegisterRequest;
import com.necronet.apiregisterms.dto.ApiResponse;
import com.necronet.apiregisterms.dto.ApiUpdateRequest;
import com.necronet.apiregisterms.dto.TestRequest;
import com.necronet.apiregisterms.dto.TestResponse;
import com.necronet.apiregisterms.entity.ApiEndpoint;
import com.necronet.apiregisterms.entity.Apis;
import com.necronet.apiregisterms.entity.AuthConfig;
import com.necronet.apiregisterms.entity.AuthCredential;
import com.necronet.apiregisterms.entity.AuthType;
import com.necronet.apiregisterms.entity.Header;
import com.necronet.apiregisterms.entity.Method;
import com.necronet.apiregisterms.repository.ApisRepository;
import com.necronet.apiregisterms.repository.AuthConfigRepository;
import com.necronet.apiregisterms.repository.AuthCredentialRepository;
import com.necronet.apiregisterms.repository.HeaderRepository;
import com.necronet.apiregisterms.repository.MethodRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiServiceTest {

    @Mock
    private ApisRepository apisRepository;
    @Mock
    private MethodRepository methodRepository;
    @Mock
    private HeaderRepository headerRepository;
    @Mock
    private AuthConfigRepository authConfigRepository;
    @Mock
    private AuthCredentialRepository authCredentialRepository;
    @Mock
    private WebClient webClient;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ApiService apiService;

    @Captor
    private ArgumentCaptor<ApiEndpoint> apiEndpointCaptor;

    private Method createMethod(String name) {
        return Method.builder().id(1L).name(name).build();
    }

    private Header createHeader(String value) {
        return Header.builder().id(1L).value(value).build();
    }

    private AuthCredential createAuthCredential(String value) {
        return AuthCredential.builder().id(1L).credentialValue(value).build();
    }

    @Test
    void registerApi_withoutAuthConfig_shouldSaveApi() {
        Method postMethod = createMethod("POST");
        given(methodRepository.findByName("POST")).willReturn(Optional.of(postMethod));
        given(apisRepository.save(any(Apis.class))).willAnswer(invocation -> {
            Apis saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ApiRegisterRequest request = ApiRegisterRequest.builder()
                .method("POST")
                .url("http://test.com/api")
                .description("test endpoint")
                .pathParams("/{id}")
                .queryParams("?page=1")
                .body("{\"key\":\"value\"}")
                .build();

        Apis result = apiService.registerApi(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMethod().getName()).isEqualTo("POST");
        assertThat(result.getUrl()).isEqualTo("http://test.com/api");

        then(apisRepository).should(times(1)).save(any(Apis.class));
        then(authConfigRepository).shouldHaveNoInteractions();
    }

    @Test
    void registerApi_withAuthConfig_shouldSaveApiAndAuthConfig() {
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("Authorization");
        given(methodRepository.findByName("POST")).willReturn(Optional.of(postMethod));
        given(headerRepository.findByValue("Authorization")).willReturn(Optional.of(authHeader));
        given(apisRepository.save(any(Apis.class))).willAnswer(invocation -> {
            Apis saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ApiRegisterRequest request = ApiRegisterRequest.builder()
                .method("POST")
                .url("http://test.com/api")
                .description("with auth")
                .authType(AuthType.BEARER)
                .authValue("test-token")
                .build();

        Apis result = apiService.registerApi(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMethod().getName()).isEqualTo("POST");

        then(apisRepository).should(times(1)).save(any(Apis.class));
        then(headerRepository).should().findByValue("Authorization");
        then(authCredentialRepository).should().save(any(AuthCredential.class));
        then(authConfigRepository).should().save(any(AuthConfig.class));
    }

    @Test
    void registerApi_withAuthConfigAndApiAuth_shouldCreateAuthApi() {
        Method getMethod = createMethod("GET");
        Header authHeader = createHeader("Authorization");
        given(methodRepository.findByName("GET"))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(getMethod));
        given(headerRepository.findByValue("Authorization")).willReturn(Optional.of(authHeader));
        given(apisRepository.save(any(Apis.class)))
                .willAnswer(invocation -> {
                    Apis saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                })
                .willAnswer(invocation -> {
                    Apis saved = invocation.getArgument(0);
                    saved.setId(2L);
                    return saved;
                });

        ApiRegisterRequest authApiRequest = ApiRegisterRequest.builder()
                .method("GET")
                .url("http://auth.com/token")
                .description("auth api")
                .build();

        ApiRegisterRequest request = ApiRegisterRequest.builder()
                .method("GET")
                .url("http://test.com/api")
                .description("with auth api")
                .authType(AuthType.BEARER)
                .authValue("test-token")
                .apiAuth(authApiRequest)
                .build();

        Apis result = apiService.registerApi(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getAuthApi()).isNotNull();
        assertThat(result.getAuthApi().getId()).isEqualTo(1L);

        then(apisRepository).should(times(2)).save(any(Apis.class));
    }

    @Test
    void toResponse_withApiEndpoint_shouldMapAllFields() {
        Method getMethod = createMethod("GET");
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .id(1L)
                .method(getMethod)
                .url("http://test.com/api")
                .description("test")
                .pathParams("/{id}")
                .queryParams("?page=1")
                .body("{\"key\":\"value\"}")
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();

        ApiResponse response = apiService.toResponse(endpoint);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getMethod()).isEqualTo("GET");
        assertThat(response.getUrl()).isEqualTo("http://test.com/api");
        assertThat(response.getPathParams()).isEqualTo("/{id}");
        assertThat(response.getQueryParams()).isEqualTo("?page=1");
        assertThat(response.getBody()).isEqualTo("{\"key\":\"value\"}");
        assertThat(response.getAuthType()).isNull();
    }

    @Test
    void toResponse_withAuthApiAndAuthConfig_shouldMapAuthFields() {
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("X-API-Key");
        AuthCredential credential = createAuthCredential("my-api-key");

        AuthConfig authConfig = AuthConfig.builder()
                .id(1L)
                .authType(AuthType.API_KEY)
                .header(authHeader)
                .authCredential(credential)
                .build();

        Apis authApi = Apis.builder().id(2L).url("http://auth.com/token").build();

        ApiEndpoint endpoint = ApiEndpoint.builder()
                .id(1L)
                .method(postMethod)
                .url("http://test.com/api")
                .description("test")
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .authApi(authApi)
                .authConfig(authConfig)
                .build();

        ApiResponse response = apiService.toResponse(endpoint);

        assertThat(response.getAuthApiId()).isEqualTo(2L);
        assertThat(response.getAuthApiUrl()).isEqualTo("http://auth.com/token");
        assertThat(response.getAuthType()).isEqualTo(AuthType.API_KEY);
        assertThat(response.getAuthHeader()).isEqualTo("X-API-Key");
        assertThat(response.getAuthValue()).isEqualTo("my-api-key");
    }

    @Test
    void getAuthValue_whenApiHasAuthApiWithCredential_shouldReturnCredential() {
        AuthCredential credential = createAuthCredential("secret-token");
        AuthConfig authConfig = AuthConfig.builder().authCredential(credential).build();
        Apis authApi = Apis.builder().id(2L).authConfig(authConfig).build();
        Apis api = Apis.builder().id(1L).authApi(authApi).build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(api));

        String result = apiService.getAuthValue(1L);

        assertThat(result).isEqualTo("secret-token");
    }

    @Test
    void getAuthValue_whenApiHasNoAuthApi_shouldReturnNull() {
        Apis api = Apis.builder().id(1L).build();
        given(apisRepository.findById(1L)).willReturn(Optional.of(api));

        String result = apiService.getAuthValue(1L);

        assertThat(result).isNull();
    }

    @Test
    void getAuthValue_whenApiNotFound_shouldThrow() {
        given(apisRepository.findById(1L)).willReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> apiService.getAuthValue(1L));
    }

    @Test
    void getAuthApiResponse_whenApiHasAuthApi_shouldReturnResponse() {
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("Authorization");
        AuthCredential credential = createAuthCredential("token123");

        AuthConfig authConfig = AuthConfig.builder()
                .authType(AuthType.BEARER)
                .header(authHeader)
                .authCredential(credential)
                .build();

        ApiEndpoint authApi = ApiEndpoint.builder()
                .id(2L)
                .method(postMethod)
                .url("http://auth.com/token")
                .description("auth api")
                .pathParams("/v2")
                .queryParams("?grant=client")
                .body("{\"client\":\"id\"}")
                .authConfig(authConfig)
                .build();

        Apis api = Apis.builder().id(1L).authApi(authApi).build();
        given(apisRepository.findById(1L)).willReturn(Optional.of(api));

        ApiResponse response = apiService.getAuthApiResponse(1L);

        assertThat(response).isNotNull();
        assertThat(response.getMethod()).isEqualTo("POST");
        assertThat(response.getUrl()).isEqualTo("http://auth.com/token");
        assertThat(response.getPathParams()).isEqualTo("/v2");
        assertThat(response.getQueryParams()).isEqualTo("?grant=client");
        assertThat(response.getBody()).isEqualTo("{\"client\":\"id\"}");
        assertThat(response.getAuthType()).isEqualTo(AuthType.BEARER);
        assertThat(response.getAuthHeader()).isEqualTo("Authorization");
        assertThat(response.getAuthValue()).isEqualTo("token123");
    }

    @Test
    void getAuthApiResponse_whenApiHasNoAuthApi_shouldReturnNull() {
        Apis api = Apis.builder().id(1L).build();
        given(apisRepository.findById(1L)).willReturn(Optional.of(api));

        ApiResponse response = apiService.getAuthApiResponse(1L);

        assertThat(response).isNull();
    }

    @Test
    void getAuthApiResponse_whenApiNotFound_shouldThrow() {
        given(apisRepository.findById(1L)).willReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> apiService.getAuthApiResponse(1L));
    }

    @Test
    void updateApi_whenFound_shouldUpdateFields() {
        Method getMethod = createMethod("GET");
        ApiEndpoint existing = ApiEndpoint.builder()
                .id(1L)
                .method(getMethod)
                .url("http://test.com/api")
                .description("original")
                .pathParams("/old")
                .queryParams("?old=true")
                .body("old body")
                .createdAt(LocalDateTime.now())
                .build();

        Method putMethod = createMethod("PUT");
        given(apisRepository.findById(1L)).willReturn(Optional.of(existing));
        given(methodRepository.findByName("PUT")).willReturn(Optional.of(putMethod));
        given(apisRepository.save(any(Apis.class))).willAnswer(invocation -> invocation.getArgument(0));

        Header authHeader = createHeader("Authorization");
        given(headerRepository.findByValue("Authorization")).willReturn(Optional.of(authHeader));

        ApiUpdateRequest request = ApiUpdateRequest.builder()
                .method("PUT")
                .url("http://test.com/updated")
                .description("updated")
                .pathParams("/new")
                .queryParams("?new=true")
                .body("new body")
                .authType(AuthType.BEARER)
                .authValue("new-token")
                .authHeader("Authorization")
                .build();

        Apis updated = apiService.updateApi(1L, request);

        assertThat(updated).isNotNull();
        assertThat(updated.getMethod().getName()).isEqualTo("PUT");
        assertThat(updated.getUrl()).isEqualTo("http://test.com/updated");
        assertThat(((ApiEndpoint) updated).getPathParams()).isEqualTo("/new");
        assertThat(((ApiEndpoint) updated).getBody()).isEqualTo("new body");
    }

    @Test
    void updateApi_whenNotFound_shouldReturnNull() {
        given(apisRepository.findById(1L)).willReturn(Optional.empty());

        Apis result = apiService.updateApi(1L, ApiUpdateRequest.builder().build());

        assertThat(result).isNull();
    }

    @Test
    void updateAuthApi_whenFound_shouldUpdateAuthApiFields() {
        ApiEndpoint authApi = ApiEndpoint.builder()
                .id(2L)
                .url("http://auth.com/token")
                .description("original auth")
                .build();

        Apis api = Apis.builder().id(1L).authApi(authApi).build();

        Method postMethod = createMethod("POST");
        given(apisRepository.findById(1L)).willReturn(Optional.of(api));
        given(methodRepository.findByName("POST")).willReturn(Optional.of(postMethod));
        given(apisRepository.save(any(Apis.class))).willAnswer(invocation -> invocation.getArgument(0));

        ApiUpdateRequest request = ApiUpdateRequest.builder()
                .method("POST")
                .url("http://auth.com/updated-token")
                .description("updated auth")
                .build();

        ApiResponse result = apiService.updateAuthApi(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getMethod()).isEqualTo("POST");
        assertThat(result.getUrl()).isEqualTo("http://auth.com/updated-token");
    }

    @Test
    void updateAuthApi_whenApiNotFound_shouldReturnNull() {
        given(apisRepository.findById(1L)).willReturn(Optional.empty());

        ApiResponse result = apiService.updateAuthApi(1L, ApiUpdateRequest.builder().build());

        assertThat(result).isNull();
    }

    @Test
    void updateAuthApi_whenNoAuthApi_shouldReturnNull() {
        Apis api = Apis.builder().id(1L).build();
        given(apisRepository.findById(1L)).willReturn(Optional.of(api));

        ApiResponse result = apiService.updateAuthApi(1L, ApiUpdateRequest.builder().build());

        assertThat(result).isNull();
    }

    @Test
    void testApi_whenNotFound_shouldReturn404() {
        given(apisRepository.findById(1L)).willReturn(Optional.empty());

        TestResponse response = apiService.testApi(1L, TestRequest.builder().build());

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getError()).isEqualTo("API not found");
    }

    @Test
    void testApi_whenFound_shouldExecuteRequest() {
        Method getMethod = createMethod("GET");
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .id(1L)
                .method(getMethod)
                .url("http://test.com/api")
                .description("test")
                .pathParams("/{id}")
                .queryParams("?page=1")
                .body("{\"key\":\"value\"}")
                .createdAt(LocalDateTime.now())
                .build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(endpoint));

        WebClient.RequestBodyUriSpec requestBodyUriSpec = org.mockito.Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = org.mockito.Mockito.mock(WebClient.RequestBodySpec.class);

        doReturn(requestBodyUriSpec).when(webClient).method(org.mockito.ArgumentMatchers.any());
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).header(anyString(), anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(org.mockito.ArgumentMatchers.any());
        doReturn(requestBodySpec).when(requestBodySpec).bodyValue(org.mockito.ArgumentMatchers.any());

        reactor.core.publisher.Mono<TestResponse> responseMono = org.mockito.Mockito.mock(reactor.core.publisher.Mono.class);
        TestResponse expectedResponse = TestResponse.builder()
                .statusCode(200)
                .body("{\"result\":\"ok\"}")
                .responseTimeMs(100L)
                .timestamp(LocalDateTime.now())
                .build();

        doReturn(responseMono).when(requestBodySpec).exchangeToMono(org.mockito.ArgumentMatchers.any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn(expectedResponse).when(responseMono).block();

        TestResponse response = apiService.testApi(1L, TestRequest.builder()
                .pathParams("/123")
                .queryParams("?filter=active")
                .body("{\"input\":\"data\"}")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("{\"result\":\"ok\"}");
    }

    @Test
    void testApi_whenRequestHasNullBody_shouldUseEndpointDefaults() {
        Method getMethod = createMethod("GET");
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .id(1L)
                .method(getMethod)
                .url("http://test.com/api")
                .pathParams("/default-path")
                .queryParams("?default=true")
                .body("default body")
                .createdAt(LocalDateTime.now())
                .build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(endpoint));

        WebClient.RequestBodyUriSpec requestBodyUriSpec = org.mockito.Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = org.mockito.Mockito.mock(WebClient.RequestBodySpec.class);

        doReturn(requestBodyUriSpec).when(webClient).method(org.mockito.ArgumentMatchers.any());
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).header(anyString(), anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(org.mockito.ArgumentMatchers.any());
        doReturn(requestBodySpec).when(requestBodySpec).bodyValue(org.mockito.ArgumentMatchers.any());

        reactor.core.publisher.Mono<TestResponse> responseMono = org.mockito.Mockito.mock(reactor.core.publisher.Mono.class);
        TestResponse expectedResponse = TestResponse.builder()
                .statusCode(200)
                .body("response")
                .responseTimeMs(50L)
                .timestamp(LocalDateTime.now())
                .build();

        doReturn(responseMono).when(requestBodySpec).exchangeToMono(org.mockito.ArgumentMatchers.any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn(expectedResponse).when(responseMono).block();

        TestResponse response = apiService.testApi(1L, null);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
    }
}
