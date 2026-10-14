package com.resilire.payments.domain.payment;

import java.util.Objects;

/** Identity of the Traveller a payment belongs to — the OIDC subject. Value object. */
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
