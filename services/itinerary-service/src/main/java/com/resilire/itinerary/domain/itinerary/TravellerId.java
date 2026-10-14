package com.resilire.itinerary.domain.itinerary;

import java.util.Objects;

/**
 * Identity of the Traveller who owns the itinerary — the OIDC subject. Reads are
 * scoped to it so one Traveller never sees another's itinerary. Value object.
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
