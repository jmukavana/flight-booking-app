package com.resilire.booking.infrastructure.payment;

import com.resilire.booking.application.PaymentGateway;
import com.resilire.booking.domain.order.Money;
import com.resilire.booking.domain.order.TravellerId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory stand-in for the Payments Service (SDD §5.1). Not active in normal
 * operation — {@link HttpPaymentGateway} is the registered bean. Keep for unit
 * tests that need a controllable no-network PaymentGateway.
 */
public class StubPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StubPaymentGateway.class);

    @Override
    public String capture(String idempotencyKey, Money amount, TravellerId traveller) {
        String paymentRef = "pay_" + UUID.randomUUID();
        log.info("Stub captured {} {} (key={}) -> {}",
                amount.amount().toPlainString(), amount.currency(), idempotencyKey, paymentRef);
        return paymentRef;
    }

    @Override
    public void refund(String idempotencyKey, String paymentRef) {
        log.info("Stub refunded {} (key={})", paymentRef, idempotencyKey);
    }
}
