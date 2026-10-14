package com.resilire.itinerary.domain.itinerary;

import java.time.Instant;
import java.util.Objects;

/**
 * One leg of an itinerary (the Segment of SDD §4, denormalised into the read
 * model). Phase 1 derives a single leg from each {@code OrderCreated}; the list
 * grows as multi-segment and rebooked legs arrive in Phase 2. Value object.
 */
public record ItineraryLeg(
        String origin,
        String destination,
        Instant departure,
        Instant arrival,
        String marketingCarrier,
        String pnr) {

    public ItineraryLeg {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(departure, "departure");
        Objects.requireNonNull(arrival, "arrival");
        Objects.requireNonNull(marketingCarrier, "marketingCarrier");
        Objects.requireNonNull(pnr, "pnr");
    }
}
