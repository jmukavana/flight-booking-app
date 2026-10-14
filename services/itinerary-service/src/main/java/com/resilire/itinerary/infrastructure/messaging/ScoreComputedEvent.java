package com.resilire.itinerary.infrastructure.messaging;

/**
 * Wire shape of the Resilience Score Service's {@code ScoreComputed} integration
 * event (its {@code ScoreComputedPayload}). Consumer-side record so this service
 * depends on the published contract, not the producer's internals.
 */
public record ScoreComputedEvent(
        String eventId,
        String eventType,
        String itineraryId,
        String travellerId,
        int score,
        String band,
        String source,
        String modelVersion,
        String computedAt) {
}
