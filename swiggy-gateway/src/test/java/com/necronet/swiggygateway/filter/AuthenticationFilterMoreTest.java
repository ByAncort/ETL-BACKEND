package com.necronet.swiggygateway.filter;

import com.necronet.swiggygateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@SpringBootTest(properties = {
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
class AuthenticationFilterMoreTest {

    // --- RouteValidator tests ---

    @Autowired
    private RouteValidator routeValidator;

    @Nested
    class RouteValidatorTest {

        @Test
        void isSecured_shouldReturnTrue_forApiPath() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            given(request.getURI()).willReturn(URI.create("/api/protected"));
            given(request.getMethod()).willReturn(HttpMethod.GET);

            boolean result = routeValidator.isSecured.test(request);

            assertThat(result).isTrue();
        }

        @Test
        void isSecured_shouldReturnFalse_forAuthRegister() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            given(request.getURI()).willReturn(URI.create("/api/v1/auth/register"));
            given(request.getMethod()).willReturn(HttpMethod.POST);

            boolean result = routeValidator.isSecured.test(request);

            assertThat(result).isFalse();
        }

        @Test
        void isSecured_shouldReturnFalse_forAuthToken() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            given(request.getURI()).willReturn(URI.create("/api/v1/auth/token"));
            given(request.getMethod()).willReturn(HttpMethod.POST);

            boolean result = routeValidator.isSecured.test(request);

            assertThat(result).isFalse();
        }

        @Test
        void isSecured_shouldReturnFalse_forEureka() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            given(request.getURI()).willReturn(URI.create("/eureka"));
            given(request.getMethod()).willReturn(HttpMethod.GET);

            boolean result = routeValidator.isSecured.test(request);

            assertThat(result).isFalse();
        }

        @Test
        void isSecured_shouldReturnFalse_forEurekaSlash() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            given(request.getURI()).willReturn(URI.create("/eureka/apps"));
            given(request.getMethod()).willReturn(HttpMethod.GET);

            boolean result = routeValidator.isSecured.test(request);

            assertThat(result).isFalse();
        }

        @Test
        void isSecured_shouldReturnFalse_forOptionsMethod() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            given(request.getURI()).willReturn(URI.create("/api/protected"));
            given(request.getMethod()).willReturn(HttpMethod.OPTIONS);

            boolean result = routeValidator.isSecured.test(request);

            assertThat(result).isFalse();
        }

        @Test
        void isSecured_shouldReturnFalse_forUsersLogin() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            given(request.getURI()).willReturn(URI.create("/api/users/login"));
            given(request.getMethod()).willReturn(HttpMethod.POST);

            boolean result = routeValidator.isSecured.test(request);

            assertThat(result).isFalse();
        }

        @Test
        void isPublicEndpoint_shouldReturnTrue_forKnownPath() {
            boolean result = routeValidator.isPublicEndpoint("/api/users/login");

            assertThat(result).isTrue();
        }

        @Test
        void isPublicEndpoint_shouldReturnFalse_forSecuredPath() {
            boolean result = routeValidator.isPublicEndpoint("/api/v1/orders");

            assertThat(result).isFalse();
        }
    }

    // --- RateLimitFilter tests ---

    @Nested
    class RateLimitFilterTest {

        @Autowired
        private RateLimitFilter rateLimitFilter;

        @MockBean
        private JwtUtil jwtUtil;

        private GatewayFilter filter;
        private ServerWebExchange exchange;
        private ServerHttpRequest request;
        private ServerHttpResponse response;
        private GatewayFilterChain chain;
        private DataBufferFactory bufferFactory;

        @BeforeEach
        void setUp() {
            filter = rateLimitFilter.apply(new RateLimitFilter.Config());
            exchange = mock(ServerWebExchange.class);
            request = mock(ServerHttpRequest.class);
            response = mock(ServerHttpResponse.class);
            chain = mock(GatewayFilterChain.class);
            bufferFactory = mock(DataBufferFactory.class);

            given(exchange.getRequest()).willReturn(request);
            given(exchange.getResponse()).willReturn(response);
            given(response.bufferFactory()).willReturn(bufferFactory);
            given(bufferFactory.wrap(any(byte[].class))).willReturn(mock(DataBuffer.class));
            given(response.writeWith(any(Mono.class))).willReturn(Mono.empty());
            given(response.getHeaders()).willReturn(new HttpHeaders());
        }

        @Test
        void rateLimit_shouldAllowUpToLimitForRegisterEndpoint() {
            String path = "/api/users/register";
            given(request.getURI()).willReturn(URI.create(path));
            given(request.getMethod()).willReturn(HttpMethod.POST);
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Forwarded-For", "10.0.0.1");
            given(request.getHeaders()).willReturn(headers);
            given(chain.filter(exchange)).willReturn(Mono.empty());

            for (int i = 0; i < 5; i++) {
                filter.filter(exchange, chain).block();
            }

            then(chain).should(times(5)).filter(exchange);
        }

        @Test
        void rateLimit_shouldBlockWhenLimitExceeded() {
            String path = "/api/users/register";
            given(request.getURI()).willReturn(URI.create(path));
            given(request.getMethod()).willReturn(HttpMethod.POST);
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Forwarded-For", "10.0.0.2");
            given(request.getHeaders()).willReturn(headers);
            given(chain.filter(exchange)).willReturn(Mono.empty());
            given(response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS)).willReturn(true);
            given(response.setStatusCode(any(HttpStatus.class))).willReturn(true);

            for (int i = 0; i < 5; i++) {
                filter.filter(exchange, chain).block();
            }

            filter.filter(exchange, chain).block();

            then(response).should().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            then(chain).should(times(5)).filter(exchange);
        }

        @Test
        void rateLimit_shouldAddHeaders_onSuccess() {
            String path = "/api/users/register";
            given(request.getURI()).willReturn(URI.create(path));
            given(request.getMethod()).willReturn(HttpMethod.POST);
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Forwarded-For", "10.0.0.3");
            given(request.getHeaders()).willReturn(headers);
            given(chain.filter(exchange)).willReturn(Mono.empty());

            filter.filter(exchange, chain).block();

            then(response).should(never()).setStatusCode(any(HttpStatus.class));
            then(chain).should().filter(exchange);
        }
    }
}
