package com.necronet.swiggygateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

@Configuration
public class GlobalFilterConfig {

    @Bean
    @Order(-1)
    public GlobalFilter corsGlobalFilter() {
        return (exchange, chain) -> {
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-User-Name, X-User-Roles");
            exchange.getResponse().getHeaders().add("Access-Control-Expose-Headers", "Authorization");
            exchange.getResponse().getHeaders().add("Access-Control-Max-Age", "3600");

            if ("OPTIONS".equalsIgnoreCase(String.valueOf(exchange.getRequest().getMethod()))) {
                return Mono.empty();
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