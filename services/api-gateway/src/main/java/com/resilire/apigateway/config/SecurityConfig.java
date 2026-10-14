package com.resilire.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

/**
 * The gateway is the OIDC <em>client</em> and single front door. Unauthenticated
 * requests are redirected to Keycloak (authorization-code + PKCE); once logged
 * in, the {@code TokenRelay} default filter forwards the access token to
 * downstream services, which validate it as resource servers. Logout is
 * RP-initiated so the Keycloak session is also ended.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http, ReactiveClientRegistrationRepository clients) {
        http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/**",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessHandler(rpInitiatedLogout(clients)))
                .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }

    private ServerLogoutSuccessHandler rpInitiatedLogout(
            ReactiveClientRegistrationRepository clients) {
        OidcClientInitiatedServerLogoutSuccessHandler handler =
                new OidcClientInitiatedServerLogoutSuccessHandler(clients);
        handler.setPostLogoutRedirectUri("{baseUrl}");
        return handler;
    }
}
