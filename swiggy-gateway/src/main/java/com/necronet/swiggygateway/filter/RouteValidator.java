package com.necronet.swiggygateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
@Slf4j
public class RouteValidator {

    // These endpoints are OPEN - no authentication required
    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/token",
            "/api/v1/auth/refresh",
            "/eureka",
            "/eureka/",
            "/actuator",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/webjars",
            "/favicon.ico",
            "/health",
            "/info"
    );

    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "GET";

        // Always allow OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }

        // Check if path matches any open endpoint
        boolean isOpen = OPEN_API_ENDPOINTS.stream()
                .anyMatch(uri -> {
                    boolean match = path.equals(uri) ||
                            path.startsWith(uri + "/") ||
                            path.contains(uri);
                    if (match) {
                        log.debug("Open endpoint matched: {} for path: {}", uri, path);
                    }
                    return match;
                });

        if (isOpen) {
            log.debug("Route is OPEN (no auth): {}", path);
        } else {
            log.debug("Route is SECURED (auth required): {}", path);
        }

        return !isOpen;
    };

    public boolean isPublicEndpoint(String path) {
        return OPEN_API_ENDPOINTS.stream()
                .anyMatch(uri -> path.equals(uri) || path.startsWith(uri + "/") || path.contains(uri));
    }
}