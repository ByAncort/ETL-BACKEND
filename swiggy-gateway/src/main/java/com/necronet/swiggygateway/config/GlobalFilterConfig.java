package com.necronet.swiggygateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

@Configuration
public class GlobalFilterConfig {

    @Bean
    @Order(1)
    public GlobalFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();

            return chain.filter(exchange)
                    .then(Mono.fromRunnable(() -> {
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