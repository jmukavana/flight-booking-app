package com.resilire.resiliencescore.infrastructure.messaging;

/**
 * Wire shape of Booking's {@code OrderCreated} integration event. Consumer-side
 * record so this service depends on the published contract, not Booking's
 * internals. All fields are strings as serialised on the topic.
 */
public record OrderCreatedEvent(
        String eventId,
        String eventType,
        String orderId,
        String itineraryId,
        String travellerId,
        String provider,
        String pnr,
        String origin,
        String destination,
        String departure,
        String arrival,
        String amount,
        String currency,
        String occurredAt) {
}
