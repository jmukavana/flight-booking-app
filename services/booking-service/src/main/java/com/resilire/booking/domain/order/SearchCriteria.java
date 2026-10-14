package com.resilire.booking.domain.order;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/** Inbound query for an offer search. Value object. */
public record SearchCriteria(String origin, String destination, LocalDate departureDate) {

    public SearchCriteria {
        origin = normaliseIata(origin, "origin");
        destination = normaliseIata(destination, "destination");
        Objects.requireNonNull(departureDate, "departureDate");
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("origin and destination must differ");
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
