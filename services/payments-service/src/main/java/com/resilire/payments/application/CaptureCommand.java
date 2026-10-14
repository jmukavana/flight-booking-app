package com.resilire.payments.application;

import com.resilire.payments.domain.payment.Money;
import com.resilire.payments.domain.payment.TravellerId;
import java.util.Objects;

/**
 * Command to capture a payment. The {@code idempotencyKey} (the booking's key)
 * makes a retried capture resolve to the original ledger entry rather than charging
 * twice (SDD §5.3).
 */
public record CaptureCommand(TravellerId traveller, Money amount, String idempotencyKey) {

    public CaptureCommand {
        Objects.requireNonNull(traveller, "traveller");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
