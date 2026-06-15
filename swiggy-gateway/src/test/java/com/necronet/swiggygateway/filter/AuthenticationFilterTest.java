package com.necronet.swiggygateway.filter;

import com.necronet.swiggygateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@SpringBootTest(properties = {
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
class AuthenticationFilterTest {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @MockBean
    private RouteValidator routeValidator;

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
        filter = authenticationFilter.apply(new AuthenticationFilter.Config());
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
    void apply_whenOpenRoute_shouldSkipAuth() {
        routeValidator.isSecured = req -> false;
        given(request.getURI()).willReturn(URI.create("/api/users/login"));
        given(request.getMethod()).willReturn(HttpMethod.POST);
        given(chain.filter(exchange)).willReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        then(chain).should().filter(exchange);
        then(response).should(never()).setStatusCode(any());
    }

    @Test
    void apply_whenMissingAuthHeader_shouldReturn401() {
        routeValidator.isSecured = req -> true;
        given(request.getURI()).willReturn(URI.create("/api/protected"));
        given(request.getMethod()).willReturn(HttpMethod.GET);
        given(request.getHeaders()).willReturn(new HttpHeaders());
        given(response.setStatusCode(HttpStatus.UNAUTHORIZED)).willReturn(true);

        filter.filter(exchange, chain).block();

        then(response).should().setStatusCode(HttpStatus.UNAUTHORIZED);
        then(chain).should(never()).filter(any());
    }

    @Test
    void apply_whenInvalidAuthHeaderFormat_shouldReturn401() {
        routeValidator.isSecured = req -> true;
        given(request.getURI()).willReturn(URI.create("/api/protected"));
        given(request.getMethod()).willReturn(HttpMethod.GET);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Basic dGVzdDp0ZXN0");
        given(request.getHeaders()).willReturn(headers);
        given(response.setStatusCode(HttpStatus.UNAUTHORIZED)).willReturn(true);

        filter.filter(exchange, chain).block();

        then(response).should().setStatusCode(HttpStatus.UNAUTHORIZED);
        then(chain).should(never()).filter(any());
    }

    @Test
    void apply_whenInvalidToken_shouldReturn401() {
        routeValidator.isSecured = req -> true;
        given(request.getURI()).willReturn(URI.create("/api/protected"));
        given(request.getMethod()).willReturn(HttpMethod.GET);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        given(request.getHeaders()).willReturn(headers);
        given(jwtUtil.validateToken("invalid-token")).willReturn(false);
        given(response.setStatusCode(HttpStatus.UNAUTHORIZED)).willReturn(true);

        filter.filter(exchange, chain).block();

        then(response).should().setStatusCode(HttpStatus.UNAUTHORIZED);
        then(chain).should(never()).filter(any());
    }

    @Test
    void apply_whenUsernameNull_shouldReturn401() {
        routeValidator.isSecured = req -> true;
        given(request.getURI()).willReturn(URI.create("/api/protected"));
        given(request.getMethod()).willReturn(HttpMethod.GET);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        given(request.getHeaders()).willReturn(headers);
        given(jwtUtil.validateToken("valid-token")).willReturn(true);
        given(jwtUtil.extractUsername("valid-token")).willReturn(null);
        given(response.setStatusCode(HttpStatus.UNAUTHORIZED)).willReturn(true);

        filter.filter(exchange, chain).block();

        then(response).should().setStatusCode(HttpStatus.UNAUTHORIZED);
        then(chain).should(never()).filter(any());
    }

    @Test
    void apply_whenValidToken_shouldMutateRequestAndProceed() {
        routeValidator.isSecured = req -> true;
        given(request.getURI()).willReturn(URI.create("/api/protected"));
        given(request.getMethod()).willReturn(HttpMethod.GET);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        given(request.getHeaders()).willReturn(headers);
        given(jwtUtil.validateToken("valid-token")).willReturn(true);
        given(jwtUtil.extractUsername("valid-token")).willReturn("testuser");
        given(jwtUtil.extractRoles("valid-token")).willReturn("USER");

        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        given(request.mutate()).willReturn(requestBuilder);
        given(requestBuilder.header("X-User-Name", "testuser")).willReturn(requestBuilder);
        given(requestBuilder.header("X-User-Roles", "USER")).willReturn(requestBuilder);
        given(requestBuilder.header("X-User-Token", "valid-token")).willReturn(requestBuilder);
        given(requestBuilder.build()).willReturn(request);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        given(exchange.mutate()).willReturn(exchangeBuilder);
        given(exchangeBuilder.request(request)).willReturn(exchangeBuilder);
        given(exchangeBuilder.build()).willReturn(exchange);

        given(chain.filter(exchange)).willReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        then(requestBuilder).should().header("X-User-Name", "testuser");
        then(requestBuilder).should().header("X-User-Roles", "USER");
        then(requestBuilder).should().header("X-User-Token", "valid-token");
        then(chain).should().filter(exchange);
    }
}
