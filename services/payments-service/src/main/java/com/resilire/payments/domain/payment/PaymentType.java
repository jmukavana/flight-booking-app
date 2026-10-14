package com.resilire.payments.domain.payment;

/**
 * The kind of ledger movement. CAPTURE/REFUND cover the booking money path;
 * PAYOUT covers rights-driven compensation disbursements (Phase 5).
 */
public enum PaymentType {
    CAPTURE,
    REFUND,
    PAYOUT
}
