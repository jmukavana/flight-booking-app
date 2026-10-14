package com.resilire.ingestion.infrastructure.messaging;

import com.resilire.ingestion.domain.feed.FlightStatusEvent;

/**
 * Published language for the {@code flight.status.updated} integration event.
 * Consumed by {@code disruption-detection-service} to drive the disruption
 * state machine.
 */
public record FlightStatusPayload(
        String eventId,
        String eventType,
        String flightId,
        String origin,
        String destination,
        String status,
        String scheduledDeparture,
        String estimatedDeparture,
        long delayMinutes,
        String source,
        String normalisedAt) {

    public static final String EVENT_TYPE = "FlightStatusUpdated";
    public static final String TOPIC = "flight.status.updated";

    public static FlightStatusPayload from(FlightStatusEvent e) {
        return new FlightStatusPayload(
                java.util.UUID.randomUUID().toString(),
                EVENT_TYPE,
                e.flightId().value(),
                e.origin().value(),
                e.destination().value(),
                e.status().name(),
                e.scheduledDeparture().toString(),
                e.estimatedDeparture().toString(),
                e.delay().toMinutes(),
                e.source(),
                e.normalisedAt().toString());
    }
}
