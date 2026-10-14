package com.resilire.itinerary.infrastructure.messaging;

/**
 * Wire shape of Booking's {@code OrderCreated} integration event (its
 * {@code OrderCreatedPayload}). Kept as a separate consumer-side record so this
 * service depends on the published contract, not Booking's internals. All fields
 * are strings as serialised on the topic; timestamps are ISO-8601 instants.
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
