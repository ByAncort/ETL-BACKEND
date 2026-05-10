package com.necronet.swiggygateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL = 60_000;
    private volatile Instant lastCleanup = Instant.now();

    private static final Map<String, RateLimitConfig> ENDPOINT_CONFIGS = Map.of(
            "/api/users/register", new RateLimitConfig(5, 60, "Registro"),
            "/api/users/login", new RateLimitConfig(10, 60, "Login"),
            "/api/v1/auth/register", new RateLimitConfig(5, 60, "Registro"),
            "/api/v1/auth/token", new RateLimitConfig(10, 60, "Login")
    );

    private static final RateLimitConfig DEFAULT_CONFIG = new RateLimitConfig(100, 1, "General");

    public RateLimitFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            String clientIp = getClientIp(exchange);
            String bucketKey = clientIp + ":" + getEndpointKey(path);

            RateLimitConfig endpointConfig = getConfigForPath(path);
            RateLimitBucket bucket = buckets.computeIfAbsent(bucketKey,
                    k -> new RateLimitBucket(endpointConfig.maxRequests, endpointConfig.windowSeconds));

            if (!bucket.tryConsume()) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                return onError(exchange, "Too many requests. Please try again later.",
                        HttpStatus.TOO_MANY_REQUESTS, endpointConfig.endpointName);
            }

            addRateLimitHeaders(exchange, bucket);
            cleanupIfNeeded();

            return chain.filter(exchange);
        };
    }

    private RateLimitConfig getConfigForPath(String path) {
        for (Map.Entry<String, RateLimitConfig> entry : ENDPOINT_CONFIGS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_CONFIG;
    }

    private String getEndpointKey(String path) {
        for (String endpoint : ENDPOINT_CONFIGS.keySet()) {
            if (path.startsWith(endpoint)) {
                return endpoint;
            }
        }
        return "default";
    }

    private String getClientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void addRateLimitHeaders(org.springframework.web.server.ServerWebExchange exchange,
                                     RateLimitBucket bucket) {
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(bucket.getLimit()));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(bucket.getAvailable()));
        exchange.getResponse().getHeaders().add("X-RateLimit-Reset", String.valueOf(bucket.getResetTime()));
    }

    private void cleanupIfNeeded() {
        Instant now = Instant.now();
        if (now.toEpochMilli() - lastCleanup.toEpochMilli() > CLEANUP_INTERVAL) {
            buckets.entrySet().removeIf(entry -> entry.getValue().isExpired());
            lastCleanup = now;
        }
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange,
                               String message, HttpStatus status, String action) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("Retry-After", "60");

        String responseBody = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s for %s. Please wait 60 seconds and try again.\",\"path\":\"%s\"}",
                java.time.LocalDateTime.now(),
                status.value(),
                "Too Many Requests",
                message,
                action,
                exchange.getRequest().getURI().getPath()
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody.getBytes()))
        );
    }

    public static class Config {}

    private static class RateLimitBucket {
        private final long maxRequests;
        private final long windowMillis;
        private final AtomicLong count;
        private volatile long windowStart;

        RateLimitBucket(long maxRequests, long windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowMillis = windowSeconds * 1000;
            this.count = new AtomicLong(0);
            this.windowStart = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                count.set(1);
                return true;
            }
            long current = count.get();
            if (current < maxRequests) {
                count.incrementAndGet();
                return true;
            }
            return false;
        }

        long getLimit() {
            return maxRequests;
        }

        long getAvailable() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMillis) {
                return maxRequests;
            }
            return Math.max(0, maxRequests - count.get());
        }

        long getResetTime() {
            return windowStart + windowMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - windowStart > windowMillis * 2;
        }
    }

    private record RateLimitConfig(int maxRequests, int windowSeconds, String endpointName) {}
}