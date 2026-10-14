package com.resilire.booking.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderRecord, UUID> {

    Optional<OrderRecord> findByIdempotencyKey(String idempotencyKey);

    List<OrderRecord> findByTravellerIdOrderByPlacedAtDesc(String travellerId);
}
