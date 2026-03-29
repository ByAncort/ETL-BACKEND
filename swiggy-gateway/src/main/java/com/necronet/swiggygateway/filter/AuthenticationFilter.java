package com.necronet.swiggygateway.filter;

import com.necronet.swiggygateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator routeValidator;
    private final JwtUtil jwtUtil;

    public AuthenticationFilter(RouteValidator routeValidator, JwtUtil jwtUtil) {
        super(Config.class);
        this.routeValidator = routeValidator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            log.debug("Processing request: {} {}", method, path);

            // Skip OPTIONS requests (CORS preflight)
            if ("OPTIONS".equalsIgnoreCase(method)) {
                log.debug("Skipping OPTIONS request");
                return chain.filter(exchange);
            }

            // Check if route should be secured
            if (!routeValidator.isSecured.test(request)) {
                log.debug("Route is open, skipping authentication: {}", path);
                return chain.filter(exchange);
            }

            // Get authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            log.debug("Authorization header: {}", authHeader != null ? "present" : "missing");

            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing authorization header for path: {}", path);
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            if (!authHeader.startsWith("Bearer ")) {
                log.warn("Invalid authorization header format for path: {}", path);
                return onError(exchange, "Invalid authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7).trim();
            log.debug("Token extracted, validating...");

            try {
                // Validate token
                if (!jwtUtil.validateToken(token)) {
                    log.warn("Invalid JWT token for path: {}", path);
                    return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                }

                // Extract user info
                String username = jwtUtil.extractUsername(token);
                if (username == null || username.trim().isEmpty()) {
                    log.warn("Could not extract username from token");
                    return onError(exchange, "Invalid token payload", HttpStatus.UNAUTHORIZED);
                }

                String roles = jwtUtil.extractRoles(token);
                log.info("Authenticated user: {} with roles: {} for path: {}", username, roles, path);

                // Add user info to headers for downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Name", username)
                        .header("X-User-Roles", roles != null ? roles : "USER")
                        .header("X-User-Token", token)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.error("Authentication failed for path: {}: {}", path, e.getMessage(), e);
                return onError(exchange, "Authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        exchange.getResponse().getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        String responseBody = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                java.time.LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getURI().getPath()
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
        );
    }

    public static class Config {
        // Configuration properties can be added here
    }
}