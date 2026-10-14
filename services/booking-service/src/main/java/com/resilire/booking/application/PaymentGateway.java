package com.resilire.booking.application;

import com.resilire.booking.domain.order.Money;
import com.resilire.booking.domain.order.PaymentDeclinedException;
import com.resilire.booking.domain.order.TravellerId;

/**
 * Outbound port to the Payments Service for tokenised capture/refund (SDD §5.1).
 * Booking never sees a PAN — PCI scope stays in Payments (NFR-SEC-1). Calls are
 * keyed by {@code idempotencyKey} so a retried capture or refund does not move
 * money twice (SDD §5.3).
 */
public interface PaymentGateway {

    /**
     * Capture {@code amount} for the order keyed by {@code idempotencyKey}.
     *
     * @return an opaque payment reference for the ledger
     * @throws PaymentDeclinedException if the processor declines
     */
    String capture(String idempotencyKey, Money amount, TravellerId traveller);

    /** Refund a previously captured payment. Idempotent on {@code idempotencyKey}. */
    void refund(String idempotencyKey, String paymentRef);
}
