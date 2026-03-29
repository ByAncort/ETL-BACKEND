package com.necronet.swiggygateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient identityServiceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://IDENTITY-SERVICE")
                .build();
    }
}