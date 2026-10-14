package com.resilire.payments.infrastructure.persistence;

import com.resilire.payments.domain.payment.PaymentType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryRecord, UUID> {

    Optional<LedgerEntryRecord> findByIdempotencyKeyAndType(String idempotencyKey, PaymentType type);

    Optional<LedgerEntryRecord> findByProcessorRefAndType(String processorRef, PaymentType type);
}
