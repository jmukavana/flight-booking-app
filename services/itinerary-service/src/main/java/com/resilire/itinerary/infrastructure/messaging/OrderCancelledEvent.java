package com.resilire.itinerary.infrastructure.messaging;

/** Wire shape of Booking's {@code OrderCancelled} integration event. */
public record OrderCancelledEvent(
        String eventId,
        String eventType,
        String orderId,
        String itineraryId,
        String travellerId,
        String occurredAt) {
}
