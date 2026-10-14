package com.resilire.booking.application;

import com.resilire.booking.domain.order.TravellerId;
import java.util.Objects;

/**
 * Command to place an order for a previously searched {@code offerId}. The
 * {@code idempotencyKey} is supplied by the client and makes a retried submission
 * resolve to the same order rather than booking twice (SDD §5.3).
 */
public record PlaceOrderCommand(TravellerId traveller, String offerId, String idempotencyKey) {

    public PlaceOrderCommand {
        Objects.requireNonNull(traveller, "traveller");
        Objects.requireNonNull(offerId, "offerId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        if (offerId.isBlank()) {
            throw new IllegalArgumentException("offerId must not be blank");
        }
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
