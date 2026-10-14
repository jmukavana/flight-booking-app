package com.resilire.notification.infrastructure.messaging;

/** Wire shape of Booking's {@code OrderCancelled} integration event (consumer-side record). */
public record OrderCancelledEvent(
        String eventId,
        String eventType,
        String orderId,
        String itineraryId,
        String travellerId,
        String occurredAt) {
}
