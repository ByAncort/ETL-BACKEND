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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiServiceMoreTest {

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

    private Method createMethod(String name) {
        return Method.builder().id(1L).name(name).build();
    }

    private Header createHeader(String value) {
        return Header.builder().id(1L).value(value).build();
    }

    private AuthCredential createAuthCredential(String value) {
        return AuthCredential.builder().id(1L).credentialValue(value).build();
    }

    private void stubRealObjectMapperReadTree() {
        try {
            doAnswer(invocation -> new ObjectMapper().readTree((String) invocation.getArgument(0)))
                    .when(objectMapper).readTree(anyString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ================================================================
    // registerApi edge cases
    // ================================================================

    @Test
    void registerApi_withNullApiAuth_shouldNotCreateAuthApi() {
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
                .build();

        Apis result = apiService.registerApi(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAuthApi()).isNull();
        then(authConfigRepository).shouldHaveNoInteractions();
        then(authCredentialRepository).shouldHaveNoInteractions();
    }

    @Test
    void registerApi_withNullAuthType_shouldSkipAuthConfig() {
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
                .authType(null)
                .build();

        Apis result = apiService.registerApi(request);

        assertThat(result).isNotNull();
        assertThat(result.getAuthConfig()).isNull();
        then(authConfigRepository).shouldHaveNoInteractions();
        then(authCredentialRepository).shouldHaveNoInteractions();
    }

    @Test
    void registerApi_withAuthTypeNONE_shouldSkipAuthConfig() {
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
                .authType(AuthType.NONE)
                .build();

        Apis result = apiService.registerApi(request);

        assertThat(result).isNotNull();
        assertThat(result.getAuthConfig()).isNull();
        then(authConfigRepository).shouldHaveNoInteractions();
        then(authCredentialRepository).shouldHaveNoInteractions();
    }

    // ================================================================
    // getAuthValue edge cases
    // ================================================================

    @Test
    void getAuthValue_whenAuthConfigIsNull_shouldReturnNull() {
        Apis authApi = Apis.builder().id(2L).url("http://auth.com/token").build();
        Apis api = Apis.builder().id(1L).authApi(authApi).build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(api));

        String result = apiService.getAuthValue(1L);

        assertThat(result).isNull();
    }

    @Test
    void getAuthValue_whenCredentialIsNull_shouldReturnNull() {
        AuthConfig authConfig = AuthConfig.builder().authType(AuthType.BEARER).build();
        Apis authApi = Apis.builder().id(2L).url("http://auth.com/token").authConfig(authConfig).build();
        Apis api = Apis.builder().id(1L).authApi(authApi).build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(api));

        String result = apiService.getAuthValue(1L);

        assertThat(result).isNull();
    }

    // ================================================================
    // updateApi edge cases
    // ================================================================

    @Test
    void updateApi_withPartialFields_shouldUpdateOnlyProvidedFields() {
        Method getMethod = createMethod("GET");
        ApiEndpoint existing = ApiEndpoint.builder()
                .id(1L)
                .method(getMethod)
                .url("http://test.com/api")
                .description("original")
                .pathParams("/{id}")
                .queryParams("?page=1")
                .body("{\"key\":\"value\"}")
                .createdAt(LocalDateTime.now())
                .build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(existing));
        given(apisRepository.save(any(Apis.class))).willAnswer(invocation -> invocation.getArgument(0));

        ApiUpdateRequest request = ApiUpdateRequest.builder()
                .url("http://test.com/updated")
                .description("updated desc")
                .build();

        Apis result = apiService.updateApi(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getUrl()).isEqualTo("http://test.com/updated");
        assertThat(result.getDescription()).isEqualTo("updated desc");
        assertThat(result.getMethod().getName()).isEqualTo("GET");
        assertThat(((ApiEndpoint) result).getPathParams()).isEqualTo("/{id}");
        assertThat(((ApiEndpoint) result).getQueryParams()).isEqualTo("?page=1");
        assertThat(((ApiEndpoint) result).getBody()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void updateApi_withAuthTypeNONE_shouldSkipAuthConfigUpdate() {
        Method getMethod = createMethod("GET");
        Header authHeader = createHeader("Authorization");
        AuthCredential credential = createAuthCredential("existing-token");
        AuthConfig existingAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER)
                .header(authHeader)
                .authCredential(credential)
                .build();
        ApiEndpoint existing = ApiEndpoint.builder()
                .id(1L)
                .method(getMethod)
                .url("http://test.com/api")
                .description("original")
                .authConfig(existingAuthConfig)
                .createdAt(LocalDateTime.now())
                .build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(existing));
        given(apisRepository.save(any(Apis.class))).willAnswer(invocation -> invocation.getArgument(0));

        ApiUpdateRequest request = ApiUpdateRequest.builder()
                .authType(AuthType.NONE)
                .build();

        Apis result = apiService.updateApi(1L, request);

        assertThat(result).isNotNull();
        then(authConfigRepository).shouldHaveNoInteractions();
        assertThat(result.getAuthConfig()).isSameAs(existingAuthConfig);
        assertThat(result.getAuthConfig().getAuthType()).isEqualTo(AuthType.BEARER);
    }

    // ================================================================
    // updateAuthApi edge cases
    // ================================================================

    @Test
    void updateAuthApi_withPartialFields_shouldUpdateOnlyProvidedFields() {
        Method postMethod = createMethod("POST");
        ApiEndpoint authApi = ApiEndpoint.builder()
                .id(2L)
                .method(postMethod)
                .url("http://auth.com/token")
                .description("original auth")
                .pathParams("/v1")
                .body("{\"client\":\"id\"}")
                .createdAt(LocalDateTime.now())
                .build();

        Apis api = Apis.builder().id(1L).authApi(authApi).build();
        given(apisRepository.findById(1L)).willReturn(Optional.of(api));
        given(apisRepository.save(any(Apis.class))).willAnswer(invocation -> invocation.getArgument(0));

        ApiUpdateRequest request = ApiUpdateRequest.builder()
                .url("http://auth.com/v2/token")
                .description("updated auth desc")
                .build();

        ApiResponse result = apiService.updateAuthApi(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getUrl()).isEqualTo("http://auth.com/v2/token");
        assertThat(result.getDescription()).isEqualTo("updated auth desc");
        assertThat(result.getMethod()).isEqualTo("POST");
    }

    // ================================================================
    // toResponse edge cases
    // ================================================================

    @Test
    void toResponse_withNullMethod_shouldMapMethodAsNull() {
        Apis api = Apis.builder()
                .id(1L)
                .url("http://test.com/api")
                .build();

        ApiResponse response = apiService.toResponse(api);

        assertThat(response.getMethod()).isNull();
        assertThat(response.getUrl()).isEqualTo("http://test.com/api");
    }

    @Test
    void toResponse_withAuthConfigNullHeader_shouldMapAuthHeaderAsNull() {
        AuthConfig authConfig = AuthConfig.builder()
                .authType(AuthType.API_KEY)
                .header(null)
                .build();

        Apis api = Apis.builder()
                .id(1L)
                .url("http://test.com/api")
                .authConfig(authConfig)
                .build();

        ApiResponse response = apiService.toResponse(api);

        assertThat(response.getAuthType()).isEqualTo(AuthType.API_KEY);
        assertThat(response.getAuthHeader()).isNull();
    }

    // ================================================================
    // executeRequest edge cases
    // ================================================================

    @Test
    void executeRequest_whenApiNotFound_shouldThrow() {
        given(apisRepository.findById(99L)).willReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> apiService.executeRequest(99L));
        assertThat(ex.getMessage()).contains("API not found");
    }

    // ================================================================
    // refreshApiToken — canRefreshToken / no-op edge cases
    // ================================================================

    @Test
    void refreshApiToken_whenAuthApiNullAndNoTokenConfig_shouldDoNothing() {
        Method getMethod = createMethod("GET");
        Apis api = Apis.builder().id(1L).method(getMethod).url("http://test.com/api").build();

        apiService.refreshApiToken(api);

        then(authCredentialRepository).shouldHaveNoInteractions();
        then(authConfigRepository).shouldHaveNoInteractions();
    }

    @Test
    void refreshApiToken_whenAuthApiNullButHasTokenConfig_shouldCallOAuth2() {
        AuthCredential credential = createAuthCredential("old-token");
        AuthConfig authConfig = AuthConfig.builder()
                .authType(AuthType.OAUTH2)
                .authCredential(credential)
                .tokenEndpoint("http://auth.com/oauth2/token")
                .username("client-id")
                .password("client-secret")
                .build();

        ApiEndpoint api = ApiEndpoint.builder()
                .id(1L)
                .method(createMethod("GET"))
                .url("http://test.com/api")
                .authConfig(authConfig)
                .createdAt(LocalDateTime.now())
                .build();

        WebClient.RequestBodyUriSpec postUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec postBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> postHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        reactor.core.publisher.Mono<String> responseMono = mock(reactor.core.publisher.Mono.class);

        doReturn(postUriSpec).when(webClient).post();
        doReturn(postBodySpec).when(postUriSpec).uri(anyString());
        doReturn(postBodySpec).when(postBodySpec).header(anyString(), anyString());
        doReturn(postHeadersSpec).when(postBodySpec).bodyValue(anyString());
        doReturn(responseSpec).when(postHeadersSpec).retrieve();
        doReturn(responseMono).when(responseSpec).bodyToMono(String.class);
        doReturn("{\"access_token\":\"oauth2-new-token\"}").when(responseMono).block();

        stubRealObjectMapperReadTree();

        apiService.refreshApiToken(api);

        then(webClient).should(times(1)).post();
        then(postUriSpec).should(times(1)).uri("http://auth.com/oauth2/token");
        then(authCredentialRepository).should(times(1)).save(any(AuthCredential.class));
        assertThat(credential.getCredentialValue()).isEqualTo("oauth2-new-token");
    }

    // ================================================================
    // resolveAuthHeaders via refreshApiToken
    // ================================================================

    @Test
    void refreshApiToken_withBearerAuth_shouldSendBearerHeader() {
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("Authorization");
        AuthCredential credential = createAuthCredential("my-bearer-token");
        AuthConfig authApiAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER)
                .header(authHeader)
                .authCredential(credential)
                .build();
        Apis authApi = Apis.builder().id(2L).method(postMethod).url("http://auth.com/token")
                .authConfig(authApiAuthConfig).build();

        AuthCredential mainCredential = createAuthCredential("old-token");
        AuthConfig mainAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(authHeader).authCredential(mainCredential).build();
        Apis api = Apis.builder().id(1L).method(postMethod).url("http://test.com/api")
                .authApi(authApi).authConfig(mainAuthConfig).build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        reactor.core.publisher.Mono<String> responseMono = mock(reactor.core.publisher.Mono.class);

        doReturn(uriSpec).when(webClient).method(any());
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).header(anyString(), anyString());
        doReturn(responseMono).when(bodySpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn("{\"access_token\":\"refreshed\"}").when(responseMono).block();

        stubRealObjectMapperReadTree();

        apiService.refreshApiToken(api);

        then(bodySpec).should(times(1)).header("Authorization", "Bearer my-bearer-token");
        assertThat(mainCredential.getCredentialValue()).isEqualTo("refreshed");
    }

    @Test
    void refreshApiToken_withBasicAuthUserPass_shouldBase64Encode() {
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("Authorization");
        AuthCredential credential = createAuthCredential("user:pass");
        AuthConfig authApiAuthConfig = AuthConfig.builder()
                .authType(AuthType.BASIC)
                .header(authHeader)
                .authCredential(credential)
                .build();
        Apis authApi = Apis.builder().id(2L).method(postMethod).url("http://auth.com/token")
                .authConfig(authApiAuthConfig).build();

        AuthCredential mainCredential = createAuthCredential("old");
        AuthConfig mainAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(authHeader).authCredential(mainCredential).build();
        Apis api = Apis.builder().id(1L).method(postMethod).url("http://test.com/api")
                .authApi(authApi).authConfig(mainAuthConfig).build();

        String expectedEncoded = "Basic " + Base64.getEncoder()
                .encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        String base64Part = expectedEncoded.substring(6);

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        reactor.core.publisher.Mono<String> responseMono = mock(reactor.core.publisher.Mono.class);

        doReturn(uriSpec).when(webClient).method(any());
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).header(anyString(), anyString());
        doReturn(responseMono).when(bodySpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn("{\"access_token\":\"t\"}").when(responseMono).block();

        stubRealObjectMapperReadTree();

        apiService.refreshApiToken(api);

        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
        then(bodySpec).should(times(1)).header(anyString(), headerValueCaptor.capture());
        String capturedValue = headerValueCaptor.getValue();
        assertThat(capturedValue).startsWith("Basic ");
        String capturedBase64 = capturedValue.substring(6);
        String decoded = new String(Base64.getDecoder().decode(capturedBase64), StandardCharsets.UTF_8);
        assertThat(decoded).isEqualTo("user:pass");
    }

    @Test
    void refreshApiToken_withBasicAlreadyEncoded_shouldNotDoubleEncode() {
        String preEncoded = Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("Authorization");
        AuthCredential credential = createAuthCredential(preEncoded);
        AuthConfig authApiAuthConfig = AuthConfig.builder()
                .authType(AuthType.BASIC)
                .header(authHeader)
                .authCredential(credential)
                .build();
        Apis authApi = Apis.builder().id(2L).method(postMethod).url("http://auth.com/token")
                .authConfig(authApiAuthConfig).build();

        AuthCredential mainCredential = createAuthCredential("old");
        AuthConfig mainAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(authHeader).authCredential(mainCredential).build();
        Apis api = Apis.builder().id(1L).method(postMethod).url("http://test.com/api")
                .authApi(authApi).authConfig(mainAuthConfig).build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        reactor.core.publisher.Mono<String> responseMono = mock(reactor.core.publisher.Mono.class);

        doReturn(uriSpec).when(webClient).method(any());
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).header(anyString(), anyString());
        doReturn(responseMono).when(bodySpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn("{\"access_token\":\"t\"}").when(responseMono).block();

        apiService.refreshApiToken(api);

        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
        then(bodySpec).should(times(1)).header(anyString(), headerValueCaptor.capture());
        String capturedValue = headerValueCaptor.getValue();
        assertThat(capturedValue).isEqualTo("Basic " + preEncoded);
    }

    @Test
    void refreshApiToken_withApiKeyAuth_shouldSendApiKeyAsIs() {
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("X-Api-Key");
        AuthCredential credential = createAuthCredential("sk-test-12345");
        AuthConfig authApiAuthConfig = AuthConfig.builder()
                .authType(AuthType.API_KEY)
                .header(authHeader)
                .authCredential(credential)
                .build();
        Apis authApi = Apis.builder().id(2L).method(postMethod).url("http://auth.com/token")
                .authConfig(authApiAuthConfig).build();

        AuthCredential mainCredential = createAuthCredential("old");
        AuthConfig mainAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(createHeader("Authorization")).authCredential(mainCredential).build();
        Apis api = Apis.builder().id(1L).method(postMethod).url("http://test.com/api")
                .authApi(authApi).authConfig(mainAuthConfig).build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        reactor.core.publisher.Mono<String> responseMono = mock(reactor.core.publisher.Mono.class);

        doReturn(uriSpec).when(webClient).method(any());
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).header(anyString(), anyString());
        doReturn(responseMono).when(bodySpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn("{\"access_token\":\"t\"}").when(responseMono).block();

        apiService.refreshApiToken(api);

        then(bodySpec).should(times(1)).header("X-Api-Key", "sk-test-12345");
    }

    @Test
    void refreshApiToken_withNullCredential_shouldNotSendAuthHeader() {
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("Authorization");
        AuthConfig authApiAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER)
                .header(authHeader)
                .authCredential(null)
                .build();
        Apis authApi = Apis.builder().id(2L).method(postMethod).url("http://auth.com/token")
                .authConfig(authApiAuthConfig).build();

        Apis api = Apis.builder().id(1L).method(postMethod).url("http://test.com/api")
                .authApi(authApi).build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        reactor.core.publisher.Mono<String> responseMono = mock(reactor.core.publisher.Mono.class);

        doReturn(uriSpec).when(webClient).method(any());
        doReturn(bodySpec).when(uriSpec).uri(anyString());

        apiService.refreshApiToken(api);

        then(bodySpec).should(never()).header(anyString(), anyString());
    }

    // ================================================================
    // extractTokenFromResponse via refreshApiToken
    // ================================================================

    @Test
    void refreshApiToken_withNestedDataToken_shouldExtractNestedToken() {
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("Authorization");
        AuthCredential credential = createAuthCredential("irrelevant");
        AuthConfig authApiAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(authHeader).authCredential(credential).build();
        Apis authApi = Apis.builder().id(2L).method(postMethod).url("http://auth.com/token")
                .authConfig(authApiAuthConfig).build();

        AuthCredential mainCredential = createAuthCredential("old-token");
        AuthConfig mainAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(authHeader).authCredential(mainCredential).build();
        Apis api = Apis.builder().id(1L).method(postMethod).url("http://test.com/api")
                .authApi(authApi).authConfig(mainAuthConfig).build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        reactor.core.publisher.Mono<String> responseMono = mock(reactor.core.publisher.Mono.class);

        doReturn(uriSpec).when(webClient).method(any());
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).header(anyString(), anyString());
        doReturn(responseMono).when(bodySpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn("{\"data\":{\"token\":\"nested-value-456\"}}").when(responseMono).block();

        stubRealObjectMapperReadTree();

        apiService.refreshApiToken(api);

        then(authCredentialRepository).should(times(1)).save(any(AuthCredential.class));
        assertThat(mainCredential.getCredentialValue()).isEqualTo("nested-value-456");
    }

    @Test
    void refreshApiToken_withPlainJwtResponse_shouldReturnJwt() {
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0";
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("Authorization");
        AuthCredential credential = createAuthCredential("irrelevant");
        AuthConfig authApiAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(authHeader).authCredential(credential).build();
        Apis authApi = Apis.builder().id(2L).method(postMethod).url("http://auth.com/token")
                .authConfig(authApiAuthConfig).build();

        AuthCredential mainCredential = createAuthCredential("old-jwt");
        AuthConfig mainAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(authHeader).authCredential(mainCredential).build();
        Apis api = Apis.builder().id(1L).method(postMethod).url("http://test.com/api")
                .authApi(authApi).authConfig(mainAuthConfig).build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        reactor.core.publisher.Mono<String> responseMono = mock(reactor.core.publisher.Mono.class);

        doReturn(uriSpec).when(webClient).method(any());
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).header(anyString(), anyString());
        doReturn(responseMono).when(bodySpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn(jwtToken).when(responseMono).block();

        apiService.refreshApiToken(api);

        then(authCredentialRepository).should(times(1)).save(any(AuthCredential.class));
        assertThat(mainCredential.getCredentialValue()).isEqualTo(jwtToken);
    }

    @Test
    void refreshApiToken_withNoTokenInResponse_shouldNotUpdateToken() {
        Method postMethod = createMethod("POST");
        Header authHeader = createHeader("Authorization");
        AuthCredential credential = createAuthCredential("irrelevant");
        AuthConfig authApiAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(authHeader).authCredential(credential).build();
        Apis authApi = Apis.builder().id(2L).method(postMethod).url("http://auth.com/token")
                .authConfig(authApiAuthConfig).build();

        AuthCredential mainCredential = createAuthCredential("should-stay");
        AuthConfig mainAuthConfig = AuthConfig.builder()
                .authType(AuthType.BEARER).header(authHeader).authCredential(mainCredential).build();
        Apis api = Apis.builder().id(1L).method(postMethod).url("http://test.com/api")
                .authApi(authApi).authConfig(mainAuthConfig).build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        reactor.core.publisher.Mono<String> responseMono = mock(reactor.core.publisher.Mono.class);

        doReturn(uriSpec).when(webClient).method(any());
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).header(anyString(), anyString());
        doReturn(responseMono).when(bodySpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn("{\"status\":\"ok\"}").when(responseMono).block();

        apiService.refreshApiToken(api);

        then(authCredentialRepository).should(never()).save(any());
        assertThat(mainCredential.getCredentialValue()).isEqualTo("should-stay");
    }

    // ================================================================
    // testApi — buildRequest / null request edge cases
    // ================================================================

    @Test
    void testApi_withGetMethod_shouldNotSendBody() {
        Method getMethod = createMethod("GET");
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .id(1L)
                .method(getMethod)
                .url("http://test.com/api")
                .description("test")
                .createdAt(LocalDateTime.now())
                .build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(endpoint));

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        doReturn(requestBodyUriSpec).when(webClient).method(any());
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).header(anyString(), anyString());

        reactor.core.publisher.Mono<TestResponse> responseMono = mock(reactor.core.publisher.Mono.class);
        TestResponse expectedResponse = TestResponse.builder()
                .statusCode(200)
                .body("response")
                .responseTimeMs(50L)
                .timestamp(LocalDateTime.now())
                .build();

        doReturn(responseMono).when(requestBodySpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn(expectedResponse).when(responseMono).block();

        TestResponse response = apiService.testApi(1L, TestRequest.builder().build());

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        then(requestBodySpec).should(times(0)).contentType(any());
        then(requestBodySpec).should(times(0)).bodyValue(any());
    }

    @Test
    void testApi_withPostMethodAndRequestBody_shouldSendBody() {
        Method postMethod = createMethod("POST");
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .id(1L)
                .method(postMethod)
                .url("http://test.com/api")
                .description("test post")
                .body("{\"fallback\":\"body\"}")
                .createdAt(LocalDateTime.now())
                .build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(endpoint));

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);

        doReturn(requestBodyUriSpec).when(webClient).method(any());
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).header(anyString(), anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(anyString());

        reactor.core.publisher.Mono<TestResponse> responseMono = mock(reactor.core.publisher.Mono.class);
        TestResponse expectedResponse = TestResponse.builder()
                .statusCode(200)
                .body("ok")
                .responseTimeMs(30L)
                .timestamp(LocalDateTime.now())
                .build();

        doReturn(responseMono).when(requestHeadersSpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn(expectedResponse).when(responseMono).block();

        TestResponse response = apiService.testApi(1L, TestRequest.builder()
                .body("{\"input\":\"data\"}")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        then(requestBodySpec).should(times(1)).contentType(any());
        then(requestBodySpec).should(times(1)).bodyValue("{\"input\":\"data\"}");
    }

    @Test
    void testApi_withNullRequestAndEndpointNoBody_shouldNotSendBody() {
        Method getMethod = createMethod("GET");
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .id(1L)
                .method(getMethod)
                .url("http://test.com/api")
                .description("test")
                .createdAt(LocalDateTime.now())
                .build();

        given(apisRepository.findById(1L)).willReturn(Optional.of(endpoint));

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        doReturn(requestBodyUriSpec).when(webClient).method(any());
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).header(anyString(), anyString());

        reactor.core.publisher.Mono<TestResponse> responseMono = mock(reactor.core.publisher.Mono.class);
        TestResponse expectedResponse = TestResponse.builder()
                .statusCode(200)
                .body("response")
                .responseTimeMs(50L)
                .timestamp(LocalDateTime.now())
                .build();

        doReturn(responseMono).when(requestBodySpec).exchangeToMono(any());
        doReturn(responseMono).when(responseMono).timeout(any(Duration.class));
        doReturn(expectedResponse).when(responseMono).block();

        TestResponse response = apiService.testApi(1L, null);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        then(requestBodySpec).should(times(0)).bodyValue(any());
    }
}
