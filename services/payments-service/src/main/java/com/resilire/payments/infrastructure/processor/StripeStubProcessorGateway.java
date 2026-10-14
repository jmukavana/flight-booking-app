package com.resilire.payments.infrastructure.processor;

import com.resilire.payments.application.ProcessorGateway;
import com.resilire.payments.domain.payment.Money;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for the Stripe processor (SDD §7). Returns Stripe-shaped
 * references ({@code ch_*}/{@code re_*}) so the rest of the service is real and
 * runnable without a processor account; a production adapter calls Stripe with the
 * same idempotency key. No PAN ever reaches this service (driver 6).
 */
@Component
class StripeStubProcessorGateway implements ProcessorGateway {

    private static final Logger log = LoggerFactory.getLogger(StripeStubProcessorGateway.class);

    @Override
    public String capture(String idempotencyKey, Money amount) {
        String chargeRef = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        log.info("Stub captured {} {} (key={}) -> {}",
                amount.amount().toPlainString(), amount.currency(), idempotencyKey, chargeRef);
        return chargeRef;
    }

    @Override
    public String refund(String idempotencyKey, String chargeRef, Money amount) {
        String refundRef = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        log.info("Stub refunded {} {} of {} (key={}) -> {}",
                amount.amount().toPlainString(), amount.currency(), chargeRef, idempotencyKey, refundRef);
        return refundRef;
    }
}
