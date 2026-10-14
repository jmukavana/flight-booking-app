package com.resilire.booking.infrastructure.messaging;

import com.resilire.booking.domain.order.OrderDomainEvent;
import java.util.UUID;

/**
 * Published language for the {@code OrderCreated} integration event. The Itinerary
 * Service projects its read model from this; the Resilience Score Service scores
 * the itinerary off it (architecture.md §5.1, §6). Decoupled from the internal
 * domain event so the contract can evolve independently.
 */
public record OrderCreatedPayload(
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

    public static final String EVENT_TYPE = "OrderCreated";
    public static final String TOPIC = "order.created";

    public static OrderCreatedPayload from(OrderDomainEvent.OrderPlaced e) {
        return new OrderCreatedPayload(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                e.orderId().value().toString(),
                e.itineraryId().value().toString(),
                e.traveller().value(),
                e.provider(),
                e.pnr().value(),
                e.route().origin(),
                e.route().destination(),
                e.route().departure().toString(),
                e.route().arrival().toString(),
                e.amount().amount().toPlainString(),
                e.amount().currency(),
                e.occurredAt().toString());
    }
}
