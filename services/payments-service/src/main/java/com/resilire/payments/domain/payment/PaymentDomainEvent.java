package com.resilire.payments.domain.payment;

import java.time.Instant;

/**
 * Domain events raised when money moves. The application layer turns these into the
 * {@code PaymentCaptured}/{@code PaymentRefunded} integration events via the
 * transactional outbox (ADR-10), so the ledger entry and its announcement commit
 * atomically — the immutable payment audit log (driver 3).
 */
public sealed interface PaymentDomainEvent
        permits PaymentDomainEvent.PaymentCaptured, PaymentDomainEvent.PaymentRefunded {

    String processorRef();

    TravellerId traveller();

    Money amount();

    Instant occurredAt();

    static PaymentDomainEvent of(LedgerEntry entry) {
        return switch (entry.type()) {
            case CAPTURE, PAYOUT -> new PaymentCaptured(entry.processorRef(), entry.traveller(),
                    entry.amount(), entry.idempotencyKey(), entry.recordedAt());
            case REFUND -> new PaymentRefunded(entry.processorRef(), entry.traveller(),
                    entry.amount(), entry.idempotencyKey(), entry.relatedRef(), entry.recordedAt());
        };
    }

    record PaymentCaptured(String processorRef, TravellerId traveller, Money amount,
                           String idempotencyKey, Instant occurredAt) implements PaymentDomainEvent {}

    record PaymentRefunded(String processorRef, TravellerId traveller, Money amount,
                           String idempotencyKey, String capturedRef, Instant occurredAt)
            implements PaymentDomainEvent {}
}
