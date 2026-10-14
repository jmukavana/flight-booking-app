package com.resilire.payments.domain.payment;

/** Raised when a refund references a capture that does not exist for the Traveller. */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String processorRef) {
        super("Captured payment not found: " + processorRef);
    }
}
