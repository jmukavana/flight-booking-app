package com.resilire.payments.application;

import com.resilire.payments.domain.payment.Money;
import com.resilire.payments.domain.payment.TravellerId;
import java.util.Objects;

/**
 * Disburse a rights-claims compensation amount to a traveller. {@code claimId} is
 * the idempotency key — a retried payout for the same claim returns the original
 * ledger entry without double-paying.
 */
public record PayoutCommand(String claimId, TravellerId traveller, Money amount) {

    public PayoutCommand {
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(traveller, "traveller");
        Objects.requireNonNull(amount, "amount");
        if (claimId.isBlank()) throw new IllegalArgumentException("claimId must not be blank");
    }
}
