package com.resilire.apigateway.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuotaRegistryConfig {

    @Bean
    QuotaRegistry quotaRegistry(
            @Value("${resilire.ratelimit.default-rpm:60}") int defaultRpm,
            @Value("${resilire.ratelimit.default-burst:90}") int defaultBurst) {
        return new QuotaRegistry(defaultRpm, defaultBurst);
    }
}
