package com.resilire.payments.application;

import com.resilire.payments.domain.payment.TravellerId;
import java.util.Objects;

/**
 * Command to refund a previously captured payment, identified by its processor
 * reference. Idempotent on {@code idempotencyKey} so a retried refund does not pay
 * out twice (SDD §5.3).
 */
public record RefundCommand(TravellerId traveller, String capturedRef, String idempotencyKey) {

    public RefundCommand {
        Objects.requireNonNull(traveller, "traveller");
        Objects.requireNonNull(capturedRef, "capturedRef");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        if (capturedRef.isBlank()) {
            throw new IllegalArgumentException("capturedRef must not be blank");
        }
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
