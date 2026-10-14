package com.resilire.itinerary.application;

import com.resilire.itinerary.domain.itinerary.ItineraryId;
import java.time.Instant;
import java.util.Objects;

/** Application command derived from a consumed {@code OrderCancelled} integration event. */
public record OrderCancelledProjection(ItineraryId itineraryId, Instant occurredAt) {

    public OrderCancelledProjection {
        Objects.requireNonNull(itineraryId, "itineraryId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
