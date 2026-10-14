package com.resilire.payments.infrastructure.messaging;

import com.resilire.payments.domain.payment.PaymentDomainEvent;
import java.util.UUID;

/**
 * Published language for the {@code PaymentRefunded} integration event. {@code capturedRef}
 * links the refund to the capture it reverses, for audit reconciliation.
 */
public record PaymentRefundedPayload(
        String eventId,
        String eventType,
        String processorRef,
        String capturedRef,
        String travellerId,
        String amount,
        String currency,
        String idempotencyKey,
        String occurredAt) {

    public static final String EVENT_TYPE = "PaymentRefunded";
    public static final String TOPIC = "payment.refunded";

    public static PaymentRefundedPayload from(PaymentDomainEvent.PaymentRefunded e) {
        return new PaymentRefundedPayload(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                e.processorRef(),
                e.capturedRef(),
                e.traveller().value(),
                e.amount().amount().toPlainString(),
                e.amount().currency(),
                e.idempotencyKey(),
                e.occurredAt().toString());
    }
}
