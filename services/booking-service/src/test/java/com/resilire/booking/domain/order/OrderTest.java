package com.resilire.booking.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Aggregate invariants — pure domain, no Spring, fast. */
class OrderTest {

    private final TravellerId traveller = TravellerId.of("traveller-1");
    private final FlightOffer offer = sampleOffer();

    @Test
    void place_confirms_and_raises_OrderPlaced() {
        Order order = place();

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.provider()).isEqualTo(offer.provider());
        assertThat(order.amount()).isEqualTo(offer.price());
        assertThat(order.pullDomainEvents())
                .singleElement()
                .isInstanceOf(OrderDomainEvent.OrderPlaced.class);
    }

    @Test
    void events_drain_only_once() {
        Order order = place();
        order.pullDomainEvents();

        assertThat(order.pullDomainEvents()).isEmpty();
    }

    @Test
    void cancel_marks_cancelled_and_raises_event() {
        Order order = place();
        order.pullDomainEvents();

        order.cancel();

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.pullDomainEvents())
                .singleElement()
                .isInstanceOf(OrderDomainEvent.OrderCancelled.class);
    }

    @Test
    void cancel_is_idempotent() {
        Order order = place();
        order.cancel();
        order.pullDomainEvents();

        order.cancel();

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.pullDomainEvents()).isEmpty();
    }

    @Test
    void rehydrate_raises_no_events() {
        Order rebuilt = Order.rehydrate(OrderId.newId(), ItineraryId.newId(), traveller,
                offer.provider(), Pnr.of("ABC123"), offer.route(), offer.price(),
                "pay_1", "idem-1", OrderStatus.CONFIRMED, Instant.now());

        assertThat(rebuilt.pullDomainEvents()).isEmpty();
    }

    private Order place() {
        return Order.place(OrderId.newId(), ItineraryId.newId(), traveller, offer,
                Pnr.of("ABC123"), "pay_1", "idem-1");
    }

    private static FlightOffer sampleOffer() {
        Instant departure = Instant.parse("2026-07-01T07:30:00Z");
        Route route = new Route("LHR", "JFK", departure, departure.plus(Duration.ofHours(7)));
        return new FlightOffer("offer-1", "STUB-AIR", route, Money.of("189.00", "EUR"));
    }
}
