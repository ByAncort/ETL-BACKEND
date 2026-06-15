package com.necronet.apiregisterms.repository;

import com.necronet.apiregisterms.entity.ApiEndpoint;
import com.necronet.apiregisterms.entity.Apis;
import com.necronet.apiregisterms.entity.AuthConfig;
import com.necronet.apiregisterms.entity.AuthCredential;
import com.necronet.apiregisterms.entity.AuthType;
import com.necronet.apiregisterms.entity.Header;
import com.necronet.apiregisterms.entity.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ApisRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ApisRepository apisRepository;

    @Autowired
    private MethodRepository methodRepository;

    @Autowired
    private HeaderRepository headerRepository;

    @Autowired
    private AuthConfigRepository authConfigRepository;

    @Autowired
    private AuthCredentialRepository authCredentialRepository;

    private Method persistMethod(String name) {
        Method method = Method.builder().name(name).build();
        return em.persistAndFlush(method);
    }

    private Header persistHeader(String value) {
        Header header = Header.builder().value(value).build();
        return em.persistAndFlush(header);
    }

    private AuthCredential persistAuthCredential(String credentialValue) {
        AuthCredential credential = AuthCredential.builder()
                .credentialValue(credentialValue)
                .build();
        return em.persistAndFlush(credential);
    }

    @Test
    void saveApiEndpoint_shouldPersistWithJoinedInheritance() {
        Method method = persistMethod("POST");

        ApiEndpoint endpoint = ApiEndpoint.builder()
                .method(method)
                .url("http://test.com/api")
                .description("test endpoint")
                .pathParams("/{id}")
                .queryParams("?page=1")
                .body("{\"key\":\"value\"}")
                .createdAt(LocalDateTime.now())
                .build();

        ApiEndpoint saved = em.persistAndFlush(endpoint);
        em.clear();

        Optional<Apis> found = apisRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUrl()).isEqualTo("http://test.com/api");
        assertThat(found.get().getMethod().getName()).isEqualTo("POST");
        assertThat(found.get()).isInstanceOf(ApiEndpoint.class);
        ApiEndpoint apiEndpoint = (ApiEndpoint) found.get();
        assertThat(apiEndpoint.getPathParams()).isEqualTo("/{id}");
        assertThat(apiEndpoint.getQueryParams()).isEqualTo("?page=1");
        assertThat(apiEndpoint.getBody()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void saveApis_withAuthApi_shouldPersistSelfReference() {
        Method method = persistMethod("GET");

        Apis authApi = Apis.builder()
                .method(method)
                .url("http://auth.com/token")
                .description("auth api")
                .createdAt(LocalDateTime.now())
                .build();

        Apis savedAuthApi = em.persistAndFlush(authApi);
        em.clear();

        Method mainMethod = persistMethod("POST");
        ApiEndpoint mainApi = ApiEndpoint.builder()
                .method(mainMethod)
                .url("http://main.com/api")
                .description("main api")
                .authApi(savedAuthApi)
                .createdAt(LocalDateTime.now())
                .build();

        Apis savedMain = em.persistAndFlush(mainApi);
        em.clear();

        Optional<Apis> found = apisRepository.findById(savedMain.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAuthApi()).isNotNull();
        assertThat(found.get().getAuthApi().getId()).isEqualTo(savedAuthApi.getId());
        assertThat(found.get().getAuthApi().getUrl()).isEqualTo("http://auth.com/token");
    }

    @Test
    void saveApis_withAuthConfig_shouldPersistFullAuthChain() {
        Method method = persistMethod("POST");
        Header header = persistHeader("Authorization");
        AuthCredential credential = persistAuthCredential("test-token-value");

        ApiEndpoint endpoint = ApiEndpoint.builder()
                .method(method)
                .url("http://test.com/api")
                .description("with auth")
                .createdAt(LocalDateTime.now())
                .build();

        Apis savedApi = em.persistAndFlush(endpoint);

        AuthConfig authConfig = AuthConfig.builder()
                .api(savedApi)
                .authType(AuthType.BEARER)
                .header(header)
                .authCredential(credential)
                .createdAt(LocalDateTime.now())
                .build();

        em.persistAndFlush(authConfig);
        em.clear();

        Optional<Apis> found = apisRepository.findById(savedApi.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAuthConfig()).isNotNull();
        assertThat(found.get().getAuthConfig().getAuthType()).isEqualTo(AuthType.BEARER);
        assertThat(found.get().getAuthConfig().getHeader().getValue()).isEqualTo("Authorization");
        assertThat(found.get().getAuthConfig().getAuthCredential().getCredentialValue())
                .isEqualTo("test-token-value");
    }

    @Test
    void findByUrlContaining_shouldReturnMatchingApis() {
        Method method = persistMethod("GET");

        ApiEndpoint api1 = ApiEndpoint.builder()
                .method(method)
                .url("http://test.com/api/v1")
                .description("api v1")
                .createdAt(LocalDateTime.now())
                .build();

        ApiEndpoint api2 = ApiEndpoint.builder()
                .method(method)
                .url("http://test.com/api/v2")
                .description("api v2")
                .createdAt(LocalDateTime.now())
                .build();

        ApiEndpoint api3 = ApiEndpoint.builder()
                .method(method)
                .url("http://other.com/service")
                .description("other")
                .createdAt(LocalDateTime.now())
                .build();

        em.persistAndFlush(api1);
        em.persistAndFlush(api2);
        em.persistAndFlush(api3);
        em.clear();

        List<Apis> result = apisRepository.findByUrlContaining("/api/v");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Apis::getUrl)
                .containsExactlyInAnyOrder("http://test.com/api/v1", "http://test.com/api/v2");
    }

    @Test
    void findByMethodName_shouldReturnMatchingApis() {
        Method getMethod = persistMethod("GET");
        Method postMethod = persistMethod("POST");

        ApiEndpoint getApi = ApiEndpoint.builder()
                .method(getMethod)
                .url("http://test.com/get")
                .description("get endpoint")
                .createdAt(LocalDateTime.now())
                .build();

        ApiEndpoint postApi = ApiEndpoint.builder()
                .method(postMethod)
                .url("http://test.com/post")
                .description("post endpoint")
                .createdAt(LocalDateTime.now())
                .build();

        ApiEndpoint anotherGet = ApiEndpoint.builder()
                .method(getMethod)
                .url("http://test.com/another-get")
                .description("another get")
                .createdAt(LocalDateTime.now())
                .build();

        em.persistAndFlush(getApi);
        em.persistAndFlush(postApi);
        em.persistAndFlush(anotherGet);
        em.clear();

        List<Apis> result = apisRepository.findByMethod_Name("GET");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Apis::getUrl)
                .containsExactlyInAnyOrder("http://test.com/get", "http://test.com/another-get");
    }

    @Test
    void saveAndFindBaseApisEntity() {
        Method method = persistMethod("DELETE");

        Apis baseApi = Apis.builder()
                .method(method)
                .url("http://test.com/delete/1")
                .description("base delete api")
                .createdAt(LocalDateTime.now())
                .build();

        Apis saved = em.persistAndFlush(baseApi);
        em.clear();

        Optional<Apis> found = apisRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUrl()).isEqualTo("http://test.com/delete/1");
        assertThat(found.get().getMethod().getName()).isEqualTo("DELETE");
        assertThat(found.get()).isNotInstanceOf(ApiEndpoint.class);
    }

    @Test
    void findAll_shouldReturnAllApis() {
        Method method = persistMethod("GET");

        Apis api1 = ApiEndpoint.builder()
                .method(method)
                .url("http://test.com/one")
                .createdAt(LocalDateTime.now())
                .build();

        Apis api2 = Apis.builder()
                .method(method)
                .url("http://test.com/two")
                .createdAt(LocalDateTime.now())
                .build();

        em.persistAndFlush(api1);
        em.persistAndFlush(api2);
        em.clear();

        List<Apis> all = apisRepository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    void deleteApi_shouldRemoveEntity() {
        Method method = persistMethod("GET");

        Apis api = Apis.builder()
                .method(method)
                .url("http://test.com/to-delete")
                .createdAt(LocalDateTime.now())
                .build();

        Apis saved = em.persistAndFlush(api);
        em.clear();

        apisRepository.deleteById(saved.getId());
        em.flush();
        em.clear();

        Optional<Apis> found = apisRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
}
