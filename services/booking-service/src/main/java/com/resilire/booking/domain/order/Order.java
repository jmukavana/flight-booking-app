package com.resilire.booking.domain.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for a flight order. Created already {@code CONFIRMED} via
 * {@link #place} — by the time the aggregate exists the provider has issued a PNR
 * and payment has been captured (the orchestration is the application service's
 * saga, see architecture.md §5.1). {@code idempotencyKey} is the dedup handle that
 * makes a retried placement a no-op rather than a double-booking (SDD §5.3).
 *
 * <p>Domain facts produced in a unit of work are drained via
 * {@link #pullDomainEvents()} for the application layer to publish through the outbox.
 */
public final class Order {

    private final OrderId id;
    private final ItineraryId itineraryId;
    private final TravellerId traveller;
    private final String provider;
    private final Pnr pnr;
    private final Route route;
    private final Money amount;
    private final String paymentRef;
    private final String idempotencyKey;
    private OrderStatus status;
    private final Instant placedAt;

    private final List<OrderDomainEvent> events = new ArrayList<>();

    private Order(OrderId id, ItineraryId itineraryId, TravellerId traveller, String provider,
                  Pnr pnr, Route route, Money amount, String paymentRef, String idempotencyKey,
                  OrderStatus status, Instant placedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.itineraryId = Objects.requireNonNull(itineraryId, "itineraryId");
        this.traveller = Objects.requireNonNull(traveller, "traveller");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.pnr = Objects.requireNonNull(pnr, "pnr");
        this.route = Objects.requireNonNull(route, "route");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.paymentRef = Objects.requireNonNull(paymentRef, "paymentRef");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.status = Objects.requireNonNull(status, "status");
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
    }

    /** Place a confirmed, paid order for {@code offer} and raise {@code OrderPlaced}. */
    public static Order place(OrderId id, ItineraryId itineraryId, TravellerId traveller,
                              FlightOffer offer, Pnr pnr, String paymentRef, String idempotencyKey) {
        Order order = new Order(id, itineraryId, traveller, offer.provider(), pnr,
                offer.route(), offer.price(), paymentRef, idempotencyKey,
                OrderStatus.CONFIRMED, Instant.now());
        order.events.add(new OrderDomainEvent.OrderPlaced(id, itineraryId, traveller,
                offer.provider(), pnr, offer.route(), offer.price(), order.placedAt));
        return order;
    }

    /** Rebuild from persisted state — raises no events. */
    public static Order rehydrate(OrderId id, ItineraryId itineraryId, TravellerId traveller,
                                  String provider, Pnr pnr, Route route, Money amount,
                                  String paymentRef, String idempotencyKey, OrderStatus status,
                                  Instant placedAt) {
        return new Order(id, itineraryId, traveller, provider, pnr, route, amount,
                paymentRef, idempotencyKey, status, placedAt);
    }

    /**
     * Cancel the order. Idempotent: cancelling an already-cancelled order is a
     * no-op (no state change, no event), so a retried cancel never double-refunds.
     */
    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            return;
        }
        status = OrderStatus.CANCELLED;
        events.add(new OrderDomainEvent.OrderCancelled(id, itineraryId, traveller, Instant.now()));
    }

    /** Drains domain events so the application layer can publish them once. */
    public List<OrderDomainEvent> pullDomainEvents() {
        List<OrderDomainEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }

    public OrderId id() {
        return id;
    }

    public ItineraryId itineraryId() {
        return itineraryId;
    }

    public TravellerId traveller() {
        return traveller;
    }

    public String provider() {
        return provider;
    }

    public Pnr pnr() {
        return pnr;
    }

    public Route route() {
        return route;
    }

    public Money amount() {
        return amount;
    }

    public String paymentRef() {
        return paymentRef;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant placedAt() {
        return placedAt;
    }
}
