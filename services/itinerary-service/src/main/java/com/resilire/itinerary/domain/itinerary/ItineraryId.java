package com.resilire.itinerary.domain.itinerary;

import java.util.Objects;
import java.util.UUID;

/** Identity of an itinerary — minted by Booking, carried on {@code OrderCreated}. Value object. */
public record ItineraryId(UUID value) {

    public ItineraryId {
        Objects.requireNonNull(value, "itineraryId value");
    }

    public static ItineraryId of(UUID value) {
        return new ItineraryId(value);
    }

    public static ItineraryId of(String value) {
        return new ItineraryId(UUID.fromString(value));
    }
}
