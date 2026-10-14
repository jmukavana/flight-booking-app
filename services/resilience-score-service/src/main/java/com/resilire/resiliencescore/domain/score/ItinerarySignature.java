package com.resilire.resiliencescore.domain.score;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * The structural identity of an itinerary — two itineraries with the same route,
 * carrier, date and connection shape get the same score, so the cache is keyed by
 * this rather than by a booking id (SDD §3, "caches by itinerary signature").
 * Value object.
 */
public record ItinerarySignature(String value) {

    public ItinerarySignature {
        Objects.requireNonNull(value, "signature value");
    }

    public static ItinerarySignature from(ItineraryFeatures features) {
        LocalDate departureDate = features.departure().atZone(ZoneOffset.UTC).toLocalDate();
        String value = String.join("|",
                features.origin(),
                features.destination(),
                features.marketingCarrier(),
                departureDate.toString(),
                Integer.toString(features.connections()),
                Boolean.toString(features.separateTickets()));
        return new ItinerarySignature(value);
    }
}
