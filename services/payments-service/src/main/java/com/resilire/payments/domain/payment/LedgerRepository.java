package com.resilire.payments.domain.payment;

import java.util.Optional;

/**
 * Outbound port owned by the domain. The ledger is append-only — {@code append}
 * only ever inserts. The lookups back the exactly-once guarantee: a retried
 * capture/refund resolves to the original entry instead of moving money again
 * (SDD §5.3).
 */
public interface LedgerRepository {

    void append(LedgerEntry entry);

    Optional<LedgerEntry> findByIdempotencyKeyAndType(String idempotencyKey, PaymentType type);

    Optional<LedgerEntry> findCaptureByProcessorRef(String processorRef);
}
