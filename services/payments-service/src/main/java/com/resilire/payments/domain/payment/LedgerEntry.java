package com.resilire.payments.domain.payment;

import java.time.Instant;
import java.util.Objects;

/**
 * One immutable movement in the payment ledger (SDD §4: {@code PaymentLedger} is
 * append-only — the compliance audit substrate, driver 3). Never updated or
 * deleted. {@code idempotencyKey} is the saga handle that makes a retried capture
 * or refund resolve to this same entry rather than moving money twice (SDD §5.3);
 * {@code relatedRef} links a {@code REFUND} back to the capture it reverses.
 */
public record LedgerEntry(
        LedgerEntryId id,
        PaymentType type,
        TravellerId traveller,
        Money amount,
        String processorRef,
        String idempotencyKey,
        String relatedRef,
        Instant recordedAt) {

    public LedgerEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(traveller, "traveller");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(processorRef, "processorRef");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(recordedAt, "recordedAt");
    }

    public static LedgerEntry capture(LedgerEntryId id, TravellerId traveller, Money amount,
                                      String processorRef, String idempotencyKey) {
        return new LedgerEntry(id, PaymentType.CAPTURE, traveller, amount, processorRef,
                idempotencyKey, null, Instant.now());
    }

    public static LedgerEntry refund(LedgerEntryId id, TravellerId traveller, Money amount,
                                     String processorRef, String idempotencyKey, String capturedRef) {
        return new LedgerEntry(id, PaymentType.REFUND, traveller, amount, processorRef,
                idempotencyKey, capturedRef, Instant.now());
    }

    public static LedgerEntry payout(LedgerEntryId id, TravellerId traveller, Money amount,
                                     String processorRef, String claimId) {
        return new LedgerEntry(id, PaymentType.PAYOUT, traveller, amount, processorRef,
                claimId, null, Instant.now());
    }
}
