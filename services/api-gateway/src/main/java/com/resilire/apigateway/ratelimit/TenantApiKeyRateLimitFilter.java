package com.resilire.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Per-tenant rate limiting via {@code X-Api-Key} header. Requests without a key
 * pass through (JWT-authed partner flows use a separate key; browser flows are
 * rate-limited by the Redis bucket configured in application.yml).
 *
 * <p>On quota exhaustion: responds 429 with {@code Retry-After: 60} seconds.
 */
@Component
public class TenantApiKeyRateLimitFilter
        extends AbstractGatewayFilterFactory<TenantApiKeyRateLimitFilter.Config> {

    static final String API_KEY_HEADER = "X-Api-Key";

    private final QuotaRegistry registry;

    public TenantApiKeyRateLimitFilter(QuotaRegistry registry) {
        super(Config.class);
        this.registry = registry;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
            if (apiKey == null || apiKey.isBlank()) {
                return chain.filter(exchange);
            }
            String keyHash = sha256Hex(apiKey);
            if (registry.tryConsume(keyHash)) {
                return chain.filter(exchange);
            }
            return throttled(exchange);
        };
    }

    private Mono<Void> throttled(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().set(HttpHeaders.RETRY_AFTER, "60");
        return response.setComplete();
    }

    static String sha256Hex(String raw) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static class Config {}
}
