package com.resilire.notification.infrastructure.messaging;

/** Wire shape of Booking's {@code OrderCreated} integration event (consumer-side record). */
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
