package com.resilire.booking.infrastructure.messaging;

import com.resilire.booking.domain.order.OrderDomainEvent;
import java.util.UUID;

/**
 * Published language for the {@code OrderCancelled} integration event — lets the
 * Itinerary Service retire the projection and Rights/Claims close any open flow.
 */
public record OrderCancelledPayload(
        String eventId,
        String eventType,
        String orderId,
        String itineraryId,
        String travellerId,
        String occurredAt) {

    public static final String EVENT_TYPE = "OrderCancelled";
    public static final String TOPIC = "order.cancelled";

    public static OrderCancelledPayload from(OrderDomainEvent.OrderCancelled e) {
        return new OrderCancelledPayload(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                e.orderId().value().toString(),
                e.itineraryId().value().toString(),
                e.traveller().value(),
                e.occurredAt().toString());
    }
}
