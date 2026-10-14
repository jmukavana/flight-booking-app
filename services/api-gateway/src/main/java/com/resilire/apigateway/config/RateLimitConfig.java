package com.resilire.apigateway.config;

import java.security.Principal;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rate-limit key resolver: one bucket per authenticated user (by principal name,
 * which is {@code preferred_username} from Keycloak). Unauthenticated requests
 * are blocked by the security filter before reaching this; "anonymous" is a
 * safe fallback for actuator/health probes that bypass auth.
 *
 * <p>The {@code RedisRateLimiter} is auto-configured by Spring Cloud Gateway
 * when {@code spring-boot-starter-data-redis-reactive} is on the classpath.
 * Rates are configured in {@code application.yml} (replenishRate / burstCapacity).
 */
@Configuration
public class RateLimitConfig {

    @Bean
    KeyResolver principalKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .defaultIfEmpty("anonymous");
    }
}
