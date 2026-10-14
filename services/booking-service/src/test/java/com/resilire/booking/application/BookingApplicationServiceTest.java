package com.resilire.booking.application;

import com.resilire.booking.domain.order.FlightOffer;
import com.resilire.booking.domain.order.ItineraryId;
import com.resilire.booking.domain.order.Money;
import com.resilire.booking.domain.order.Order;
import com.resilire.booking.domain.order.OrderDomainEvent;
import com.resilire.booking.domain.order.OrderId;
import com.resilire.booking.domain.order.OrderNotFoundException;
import com.resilire.booking.domain.order.OrderRepository;
import com.resilire.booking.domain.order.OrderStatus;
import com.resilire.booking.domain.order.Pnr;
import com.resilire.booking.domain.order.Route;
import com.resilire.booking.domain.order.SearchCriteria;
import com.resilire.booking.domain.order.TravellerId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingApplicationServiceTest {

    private StubOrderRepository orders;
    private StubFlightOfferProvider offerProvider;
    private StubPaymentGateway paymentGateway;
    private StubEventPublisher publisher;
    private BookingApplicationService service;

    private final TravellerId traveller = TravellerId.of("traveller-1");
    private final TravellerId other = TravellerId.of("traveller-2");

    @BeforeEach
    void setUp() {
        orders = new StubOrderRepository();
        offerProvider = new StubFlightOfferProvider();
        paymentGateway = new StubPaymentGateway();
        publisher = new StubEventPublisher();
        service = new BookingApplicationService(orders, offerProvider, paymentGateway, publisher);
    }

    @Test
    void place_order_confirms_and_publishes_OrderPlaced() {
        Order order = service.placeOrder(new PlaceOrderCommand(traveller, "offer-1", "idem-1"));

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.traveller()).isEqualTo(traveller);
        assertThat(order.provider()).isEqualTo("STUB-AIR");
        assertThat(publisher.events).singleElement().isInstanceOf(OrderDomainEvent.OrderPlaced.class);
        assertThat(orders.store).hasSize(1);
    }

    @Test
    void place_order_is_idempotent_on_idempotency_key() {
        Order first = service.placeOrder(new PlaceOrderCommand(traveller, "offer-1", "idem-1"));
        Order second = service.placeOrder(new PlaceOrderCommand(traveller, "offer-1", "idem-1"));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(offerProvider.placeCalls).isEqualTo(1);
        assertThat(paymentGateway.captureCalls).isEqualTo(1);
        assertThat(orders.store).hasSize(1);
    }

    @Test
    void cancel_order_transitions_to_cancelled_and_issues_refund() {
        Order order = service.placeOrder(new PlaceOrderCommand(traveller, "offer-1", "idem-1"));
        publisher.events.clear();

        service.cancelOrder(traveller, order.id());

        assertThat(orders.store.get(order.id().value()).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(paymentGateway.refundCalls).isEqualTo(1);
        assertThat(publisher.events).singleElement().isInstanceOf(OrderDomainEvent.OrderCancelled.class);
    }

    @Test
    void cancel_is_idempotent_no_double_refund() {
        Order order = service.placeOrder(new PlaceOrderCommand(traveller, "offer-1", "idem-1"));
        service.cancelOrder(traveller, order.id());

        service.cancelOrder(traveller, order.id());

        assertThat(paymentGateway.refundCalls).isEqualTo(1);
    }

    @Test
    void get_order_by_another_traveller_throws_not_found() {
        Order order = service.placeOrder(new PlaceOrderCommand(traveller, "offer-1", "idem-1"));

        assertThatThrownBy(() -> service.getOrder(other, order.id()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void list_orders_returns_only_travellers_orders() {
        service.placeOrder(new PlaceOrderCommand(traveller, "offer-1", "idem-1"));
        service.placeOrder(new PlaceOrderCommand(traveller, "offer-1", "idem-2"));

        List<Order> result = service.listOrders(traveller);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(o -> o.traveller().equals(traveller));
    }

    // --- stubs ---

    static class StubOrderRepository implements OrderRepository {
        final Map<java.util.UUID, Order> store = new HashMap<>();
        final Map<String, Order> byKey = new HashMap<>();

        @Override public void save(Order order) {
            store.put(order.id().value(), order);
            byKey.put(order.idempotencyKey(), order);
        }
        @Override public Optional<Order> findById(OrderId id) { return Optional.ofNullable(store.get(id.value())); }
        @Override public Optional<Order> findByIdempotencyKey(String key) { return Optional.ofNullable(byKey.get(key)); }
        @Override public List<Order> findByTraveller(TravellerId t) {
            return store.values().stream().filter(o -> o.traveller().equals(t)).toList();
        }
    }

    static class StubFlightOfferProvider implements FlightOfferProvider {
        int placeCalls = 0;

        @Override public List<FlightOffer> search(SearchCriteria c) { return List.of(sampleOffer()); }
        @Override public FlightOffer findOffer(String id) { return sampleOffer(); }
        @Override public Pnr place(FlightOffer offer, TravellerId t) { placeCalls++; return Pnr.of("PNR-001"); }
        @Override public void cancel(Pnr pnr) {}

        private FlightOffer sampleOffer() {
            Instant dep = Instant.parse("2026-09-01T06:00:00Z");
            Route route = new Route("LHR", "JFK", dep, dep.plus(Duration.ofHours(7)));
            return new FlightOffer("offer-1", "STUB-AIR", route, Money.of("189.00", "EUR"));
        }
    }

    static class StubPaymentGateway implements PaymentGateway {
        int captureCalls = 0;
        int refundCalls = 0;

        @Override public String capture(String key, Money amount, TravellerId t) {
            captureCalls++;
            return "ch_stub_" + key;
        }
        @Override public void refund(String key, String ref) { refundCalls++; }
    }

    static class StubEventPublisher implements IntegrationEventPublisher {
        final List<Object> events = new ArrayList<>();

        @Override public void publish(com.resilire.booking.domain.order.OrderDomainEvent event) {
            events.add(event);
        }
    }
}
