package com.resilire.booking.infrastructure.persistence;

import com.resilire.booking.domain.order.ItineraryId;
import com.resilire.booking.domain.order.Money;
import com.resilire.booking.domain.order.Order;
import com.resilire.booking.domain.order.OrderId;
import com.resilire.booking.domain.order.OrderRepository;
import com.resilire.booking.domain.order.Pnr;
import com.resilire.booking.domain.order.Route;
import com.resilire.booking.domain.order.TravellerId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter implementing the domain port. Translates between the
 * {@link OrderRecord} row and the {@link Order} aggregate. {@code save} is an
 * upsert keyed by the aggregate id — the order is state-stored, not event-sourced.
 */
@Repository
public class JpaOrderRepository implements OrderRepository {

    private final OrderJpaRepository rows;

    public JpaOrderRepository(OrderJpaRepository rows) {
        this.rows = rows;
    }

    @Override
    public void save(Order order) {
        rows.save(toRow(order));
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return rows.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return rows.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public List<Order> findByTraveller(TravellerId traveller) {
        return rows.findByTravellerIdOrderByPlacedAtDesc(traveller.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private OrderRecord toRow(Order order) {
        Route route = order.route();
        return new OrderRecord(
                order.id().value(),
                order.itineraryId().value(),
                order.traveller().value(),
                order.provider(),
                order.pnr().value(),
                route.origin(),
                route.destination(),
                route.departure(),
                route.arrival(),
                order.amount().amount(),
                order.amount().currency(),
                order.paymentRef(),
                order.idempotencyKey(),
                order.status(),
                order.placedAt());
    }

    private Order toDomain(OrderRecord row) {
        return Order.rehydrate(
                OrderId.of(row.getId()),
                ItineraryId.of(row.getItineraryId().toString()),
                TravellerId.of(row.getTravellerId()),
                row.getProvider(),
                Pnr.of(row.getPnr()),
                new Route(row.getOrigin(), row.getDestination(), row.getDeparture(), row.getArrival()),
                new Money(row.getAmount(), row.getCurrency()),
                row.getPaymentRef(),
                row.getIdempotencyKey(),
                row.getStatus(),
                row.getPlacedAt());
    }
}
