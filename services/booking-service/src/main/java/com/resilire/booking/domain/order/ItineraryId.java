package com.resilire.booking.domain.order;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of the itinerary an order belongs to. Booking mints this when an order
 * is placed; it travels on {@code OrderCreated} so the Itinerary Service can build
 * its read model against the same key (architecture.md §5.1). Value object.
 */
public record ItineraryId(UUID value) {

    public ItineraryId {
        Objects.requireNonNull(value, "itineraryId value");
    }

    public static ItineraryId newId() {
        return new ItineraryId(UUID.randomUUID());
    }

    public static ItineraryId of(String value) {
        return new ItineraryId(UUID.fromString(value));
    }
}
