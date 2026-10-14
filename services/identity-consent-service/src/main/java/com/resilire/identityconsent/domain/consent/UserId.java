package com.resilire.identityconsent.domain.consent;

import java.util.Objects;

/** Identity of a Traveler — the OIDC subject claim. Value object. */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "userId value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}
