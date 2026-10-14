package com.resilire.ingestion.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Ingestion service is called by trusted cluster-internal adapters (airline NDC
 * status hooks, weather data brokers) that carry no user token. All feed and
 * actuator endpoints are permitted without a Bearer token; network-layer isolation
 * (Kubernetes NetworkPolicy) enforces the trust boundary in production.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
