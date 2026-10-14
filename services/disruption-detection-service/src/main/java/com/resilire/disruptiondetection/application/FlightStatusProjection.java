package com.resilire.disruptiondetection.application;

/** Inbound projection of the {@code flight.status.updated} Kafka event from ingestion-service. */
public record FlightStatusProjection(
        String eventId,
        String flightId,
        String origin,
        String destination,
        String status,
        long delayMinutes) {
}
