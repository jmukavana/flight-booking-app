package com.resilire.identityconsent.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter =
            new KeycloakJwtAuthenticationConverter();

    private Jwt.Builder baseToken() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("traveller-1");
    }

    @Test
    void maps_keycloak_realm_roles_to_role_authorities() {
        Jwt jwt = baseToken()
                .claim("realm_access", Map.of("roles", List.of("traveller", "ops")))
                .build();

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth.getName()).isEqualTo("traveller-1");
        assertThat(AuthorityUtils.authorityListToSet(auth.getAuthorities()))
                .contains("ROLE_TRAVELLER", "ROLE_OPS");
    }

    @Test
    void token_without_realm_access_yields_no_role_authorities() {
        Jwt jwt = baseToken().claim("scope", "openid").build();

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(AuthorityUtils.authorityListToSet(auth.getAuthorities()))
                .noneMatch(a -> a.startsWith("ROLE_"));
    }
}
