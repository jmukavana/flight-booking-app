package com.resilire.resiliencescore.domain.score;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * The features that identify and characterise an itinerary for scoring (the
 * "compose features" input of SDD §3). Phase 1 keeps a small, open-OTP-derivable
 * set; the offline feature pipeline enriches this later (architecture.md §10).
 * Value object.
 */
public record ItineraryFeatures(
        String origin,
        String destination,
        String marketingCarrier,
        Instant departure,
        int connections,
        boolean separateTickets) {

    public ItineraryFeatures {
        origin = normaliseIata(origin, "origin");
        destination = normaliseIata(destination, "destination");
        Objects.requireNonNull(marketingCarrier, "marketingCarrier");
        Objects.requireNonNull(departure, "departure");
        if (marketingCarrier.isBlank()) {
            throw new IllegalArgumentException("marketingCarrier must not be blank");
        }
        if (connections < 0) {
            throw new IllegalArgumentException("connections must not be negative");
        }
        marketingCarrier = marketingCarrier.trim().toUpperCase(Locale.ROOT);
    }

    /** Convenience constructor — defaults separateTickets to false (Phase 1 single-leg orders). */
    public ItineraryFeatures(String origin, String destination, String marketingCarrier,
                             Instant departure, int connections) {
        this(origin, destination, marketingCarrier, departure, connections, false);
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
