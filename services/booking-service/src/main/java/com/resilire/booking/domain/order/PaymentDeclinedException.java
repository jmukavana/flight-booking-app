package com.resilire.booking.domain.order;

/** Raised when the payment processor declines capture for an order. */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String reason) {
        super("Payment declined: " + reason);
    }
}
