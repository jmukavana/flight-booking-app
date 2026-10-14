package com.resilire.booking.domain.order;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * The origin→destination leg of a booked offer, with its scheduled times. Carried
 * on {@code OrderCreated} so downstream contexts (Itinerary, Resilience Score) can
 * project without calling back. Value object.
 */
public record Route(String origin, String destination, Instant departure, Instant arrival) {

    public Route {
        origin = normaliseIata(origin, "origin");
        destination = normaliseIata(destination, "destination");
        Objects.requireNonNull(departure, "departure");
        Objects.requireNonNull(arrival, "arrival");
        if (arrival.isBefore(departure)) {
            throw new IllegalArgumentException("arrival must not precede departure");
        }
    }

    private static String normaliseIata(String code, String field) {
        Objects.requireNonNull(code, field);
        String trimmed = code.trim();
        if (trimmed.length() != 3) {
            throw new IllegalArgumentException(field + " must be a 3-letter IATA code");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }
}
