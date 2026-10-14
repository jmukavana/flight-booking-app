package com.resilire.booking.domain.order;

import java.util.Objects;

/**
 * Identity of the Traveller placing the order — the OIDC subject claim, never
 * taken from the request body (it is the authenticated token subject). Value object.
 */
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
