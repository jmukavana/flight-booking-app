package com.resilire.booking.domain.order;

import java.time.Instant;

/**
 * Domain events raised by the {@link Order} aggregate. The application layer
 * translates these into the {@code OrderCreated}/{@code OrderCancelled} integration
 * events (published language) via the transactional outbox (ADR-10), so the order
 * and its announcement commit atomically.
 */
public sealed interface OrderDomainEvent
        permits OrderDomainEvent.OrderPlaced, OrderDomainEvent.OrderCancelled {

    OrderId orderId();

    ItineraryId itineraryId();

    TravellerId traveller();

    Instant occurredAt();

    /** Order placed and paid — becomes the {@code OrderCreated} integration event. */
    record OrderPlaced(
            OrderId orderId,
            ItineraryId itineraryId,
            TravellerId traveller,
            String provider,
            Pnr pnr,
            Route route,
            Money amount,
            Instant occurredAt) implements OrderDomainEvent {}

    record OrderCancelled(
            OrderId orderId,
            ItineraryId itineraryId,
            TravellerId traveller,
            Instant occurredAt) implements OrderDomainEvent {}
}
