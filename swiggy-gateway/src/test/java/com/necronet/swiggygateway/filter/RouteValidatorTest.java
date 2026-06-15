package com.necronet.swiggygateway.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RouteValidatorTest {

    private final RouteValidator routeValidator = new RouteValidator();

    @Mock
    private ServerHttpRequest request;

    @Test
    void isSecured_forOpenLoginEndpoint_shouldReturnFalse() {
        given(request.getURI()).willReturn(URI.create("/api/users/login"));
        given(request.getMethod()).willReturn(HttpMethod.POST);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isFalse();
    }

    @Test
    void isSecured_forOpenRegisterEndpoint_shouldReturnFalse() {
        given(request.getURI()).willReturn(URI.create("/api/users/register"));
        given(request.getMethod()).willReturn(HttpMethod.POST);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isFalse();
    }

    @Test
    void isSecured_forOpenAuthTokenEndpoint_shouldReturnFalse() {
        given(request.getURI()).willReturn(URI.create("/api/v1/auth/token"));
        given(request.getMethod()).willReturn(HttpMethod.POST);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isFalse();
    }

    @Test
    void isSecured_forEurekaEndpoint_shouldReturnFalse() {
        given(request.getURI()).willReturn(URI.create("/eureka"));
        given(request.getMethod()).willReturn(HttpMethod.GET);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isFalse();
    }

    @Test
    void isSecured_forActuatorHealthEndpoint_shouldReturnFalse() {
        given(request.getURI()).willReturn(URI.create("/actuator/health"));
        given(request.getMethod()).willReturn(HttpMethod.GET);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isFalse();
    }

    @Test
    void isSecured_forSwaggerUiEndpoint_shouldReturnFalse() {
        given(request.getURI()).willReturn(URI.create("/swagger-ui/index.html"));
        given(request.getMethod()).willReturn(HttpMethod.GET);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isFalse();
    }

    @Test
    void isSecured_forApiDocsEndpoint_shouldReturnFalse() {
        given(request.getURI()).willReturn(URI.create("/v3/api-docs"));
        given(request.getMethod()).willReturn(HttpMethod.GET);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isFalse();
    }

    @Test
    void isSecured_forOptionsRequest_shouldReturnFalse() {
        given(request.getURI()).willReturn(URI.create("/api/protected-resource"));
        given(request.getMethod()).willReturn(HttpMethod.OPTIONS);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isFalse();
    }

    @Test
    void isSecured_forSecuredEndpoint_shouldReturnTrue() {
        given(request.getURI()).willReturn(URI.create("/api/protected-resource"));
        given(request.getMethod()).willReturn(HttpMethod.GET);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isTrue();
    }

    @Test
    void isSecured_forNullPath_shouldReturnTrue() {
        given(request.getURI()).willReturn(URI.create(""));
        given(request.getMethod()).willReturn(HttpMethod.GET);

        boolean secured = routeValidator.isSecured.test(request);

        assertThat(secured).isTrue();
    }

    @Test
    void isPublicEndpoint_forOpenEndpoint_shouldReturnTrue() {
        boolean result = routeValidator.isPublicEndpoint("/api/users/register");

        assertThat(result).isTrue();
    }

    @Test
    void isPublicEndpoint_forSecuredEndpoint_shouldReturnFalse() {
        boolean result = routeValidator.isPublicEndpoint("/api/protected");

        assertThat(result).isFalse();
    }

    @Test
    void isPublicEndpoint_forNull_shouldReturnFalse() {
        boolean result = routeValidator.isPublicEndpoint(null);

        assertThat(result).isFalse();
    }
}
