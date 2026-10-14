package com.resilire.notification.domain.notification;

import java.util.Objects;

/** Identity of the Traveller being notified — the OIDC subject carried on events. Value object. */
public record TravellerId(String value) {

    public TravellerId {
        Objects.requireNonNull(value, "travellerId value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("travellerId must not be blank");
        }
    }

    public static TravellerId of(String value) {
        return new TravellerId(value);
    }
}
