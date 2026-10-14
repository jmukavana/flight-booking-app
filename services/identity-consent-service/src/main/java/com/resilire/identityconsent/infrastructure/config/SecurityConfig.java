package com.resilire.identityconsent.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless OAuth2 resource server. JWTs are validated against the Keycloak
 * issuer configured by {@code spring.security.oauth2.resourceserver.jwt.issuer-uri};
 * {@link KeycloakJwtAuthenticationConverter} maps Keycloak realm roles to
 * authorities so RBAC works. The Traveller identity is the token subject, never
 * the request body. {@code @EnableMethodSecurity} enables {@code @PreAuthorize}
 * for the internal/ops endpoints added in later slices (FR-12 RBAC).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            KeycloakJwtAuthenticationConverter jwtAuthConverter)
            throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
