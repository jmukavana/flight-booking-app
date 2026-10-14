package com.resilire.booking.domain.order;

/**
 * Raised when an order references an offer the provider can no longer honour —
 * offers are ephemeral and may expire or be repriced between search and order.
 */
public class OfferNotFoundException extends RuntimeException {

    public OfferNotFoundException(String offerId) {
        super("Offer not available: " + offerId);
    }
}
