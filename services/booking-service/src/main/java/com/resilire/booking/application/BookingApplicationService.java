package com.resilire.booking.application;

import com.resilire.booking.domain.order.FlightOffer;
import com.resilire.booking.domain.order.ItineraryId;
import com.resilire.booking.domain.order.Order;
import com.resilire.booking.domain.order.OrderDomainEvent;
import com.resilire.booking.domain.order.OrderId;
import com.resilire.booking.domain.order.OrderNotFoundException;
import com.resilire.booking.domain.order.OrderRepository;
import com.resilire.booking.domain.order.OrderStatus;
import com.resilire.booking.domain.order.Pnr;
import com.resilire.booking.domain.order.SearchCriteria;
import com.resilire.booking.domain.order.TravellerId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the Booking context. Each write command is one unit of
 * work: do the external saga steps (provider booking, payment), build the
 * aggregate, persist it, and publish its events through the outbox — so the order
 * and its announcement commit together (ADR-10).
 *
 * <p>Exactly-once effect (SDD §5.3) rests on the client {@code idempotencyKey}: a
 * retried placement returns the original order instead of booking again. The rare
 * concurrent race surfaces as a unique-constraint violation mapped to 409-retry.
 */
@Service
public class BookingApplicationService {

    private final OrderRepository orders;
    private final FlightOfferProvider flightOffers;
    private final PaymentGateway payments;
    private final IntegrationEventPublisher events;

    public BookingApplicationService(OrderRepository orders, FlightOfferProvider flightOffers,
                                     PaymentGateway payments, IntegrationEventPublisher events) {
        this.orders = orders;
        this.flightOffers = flightOffers;
        this.payments = payments;
        this.events = events;
    }

    public List<FlightOffer> search(SearchCriteria criteria) {
        return flightOffers.search(criteria);
    }

    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        return orders.findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> bookAndRecord(command));
    }

    private Order bookAndRecord(PlaceOrderCommand command) {
        FlightOffer offer = flightOffers.findOffer(command.offerId());
        Pnr pnr = flightOffers.place(offer, command.traveller());
        String paymentRef = payments.capture(command.idempotencyKey(), offer.price(), command.traveller());

        Order order = Order.place(OrderId.newId(), ItineraryId.newId(), command.traveller(),
                offer, pnr, paymentRef, command.idempotencyKey());
        commit(order);
        return order;
    }

    @Transactional
    public Order cancelOrder(TravellerId traveller, OrderId orderId) {
        Order order = loadOwned(traveller, orderId);
        boolean wasActive = order.status() == OrderStatus.CONFIRMED;
        order.cancel();
        if (wasActive) {
            flightOffers.cancel(order.pnr());
            payments.refund(order.idempotencyKey(), order.paymentRef());
        }
        commit(order);
        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(TravellerId traveller, OrderId orderId) {
        return loadOwned(traveller, orderId);
    }

    @Transactional(readOnly = true)
    public List<Order> listOrders(TravellerId traveller) {
        return orders.findByTraveller(traveller);
    }

    /** Loads an order, hiding existence from anyone but its owner. */
    private Order loadOwned(TravellerId traveller, OrderId orderId) {
        return orders.findById(orderId)
                .filter(order -> order.traveller().equals(traveller))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void commit(Order order) {
        orders.save(order);
        for (OrderDomainEvent event : order.pullDomainEvents()) {
            events.publish(event);
        }
    }
}
