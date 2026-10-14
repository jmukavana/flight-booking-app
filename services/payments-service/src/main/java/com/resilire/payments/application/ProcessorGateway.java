package com.resilire.payments.application;

import com.resilire.payments.domain.payment.Money;

/**
 * Anti-corruption port over the card processor (Stripe + secondary, SDD §7).
 * Confines PCI scope to Payments + processor — callers pass tokens/amounts, never a
 * PAN (driver 6). Calls are keyed by {@code idempotencyKey} so the processor itself
 * does not double-charge on retry.
 */
public interface ProcessorGateway {

    /** Capture {@code amount}; returns the processor's charge reference. */
    String capture(String idempotencyKey, Money amount);

    /** Refund a previous charge; returns the processor's refund reference. */
    String refund(String idempotencyKey, String chargeRef, Money amount);
}
