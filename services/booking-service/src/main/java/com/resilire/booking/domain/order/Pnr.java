package com.resilire.booking.domain.order;

import java.util.Locale;
import java.util.Objects;

/**
 * A carrier booking reference (Passenger Name Record locator) returned by the
 * provider when an order is placed. Value object.
 */
public record Pnr(String value) {

    public Pnr {
        Objects.requireNonNull(value, "pnr value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("pnr must not be blank");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
    }

    public static Pnr of(String value) {
        return new Pnr(value);
    }
}
