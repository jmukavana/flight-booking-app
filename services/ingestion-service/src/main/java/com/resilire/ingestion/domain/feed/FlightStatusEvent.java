package com.resilire.ingestion.domain.feed;

import java.time.Duration;
import java.time.Instant;

public record FlightStatusEvent(
        FlightId flightId,
        AirportCode origin,
        AirportCode destination,
        FlightStatusCode status,
        Instant scheduledDeparture,
        Instant estimatedDeparture,
        Duration delay,
        String source,
        Instant normalisedAt) {
}
