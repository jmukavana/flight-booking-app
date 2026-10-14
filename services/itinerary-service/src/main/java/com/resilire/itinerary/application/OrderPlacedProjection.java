package com.resilire.itinerary.application;

import com.resilire.itinerary.domain.itinerary.ItineraryId;
import com.resilire.itinerary.domain.itinerary.ItineraryLeg;
import com.resilire.itinerary.domain.itinerary.Money;
import com.resilire.itinerary.domain.itinerary.TravellerId;
import java.time.Instant;
import java.util.Objects;

/**
 * Application command derived from a consumed {@code OrderCreated} integration
 * event — decoupled from the Kafka wire shape so the contract and the projection
 * can evolve independently.
 */
public record OrderPlacedProjection(
        ItineraryId itineraryId,
        TravellerId traveller,
        ItineraryLeg leg,
        Money totalAmount,
        Instant occurredAt) {

    public OrderPlacedProjection {
        Objects.requireNonNull(itineraryId, "itineraryId");
        Objects.requireNonNull(traveller, "traveller");
        Objects.requireNonNull(leg, "leg");
        Objects.requireNonNull(totalAmount, "totalAmount");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
