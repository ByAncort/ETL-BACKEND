package com.necronet.swiggygateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.Set;

@Configuration
public class GlobalFilterConfig {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173"
    );

    @Bean
    @Order(-1)
    public GlobalFilter corsGlobalFilter() {
        return (exchange, chain) -> {
            String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
            String allowedOrigin = ALLOWED_ORIGINS.contains(origin) ? origin : "http://localhost:5173";

            exchange.getResponse().getHeaders().set("Access-Control-Allow-Origin", allowedOrigin);
            exchange.getResponse().getHeaders().set("Vary", "Origin");
            exchange.getResponse().getHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponse().getHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-User-Name, X-User-Roles");
            exchange.getResponse().getHeaders().set("Access-Control-Expose-Headers", "Authorization");
            exchange.getResponse().getHeaders().set("Access-Control-Max-Age", "3600");

            if ("OPTIONS".equalsIgnoreCase(String.valueOf(exchange.getRequest().getMethod()))) {
                exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    @Bean
    @Order(1)
    public GlobalFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long endTime = System.currentTimeMillis();
                System.out.println(String.format(
                        "Request: %s %s | Status: %s | Time: %dms",
                        exchange.getRequest().getMethod(),
                        exchange.getRequest().getURI().getPath(),
                        exchange.getResponse().getStatusCode(),
                        endTime - startTime
                ));
            }));
        };
    }
}