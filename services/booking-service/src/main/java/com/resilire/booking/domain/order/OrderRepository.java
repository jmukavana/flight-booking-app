package com.resilire.booking.domain.order;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port owned by the domain, implemented by an infrastructure adapter.
 * {@link #findByIdempotencyKey} backs the exactly-once-effect guarantee: a retried
 * placement resolves to the original order instead of booking again (SDD §5.3).
 */
public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId id);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findByTraveller(TravellerId traveller);
}
