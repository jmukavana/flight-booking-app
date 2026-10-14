package com.resilire.payments.infrastructure.messaging;

import com.resilire.payments.domain.payment.PaymentDomainEvent;
import java.util.UUID;

/**
 * Published language for the {@code PaymentCaptured} integration event — feeds the
 * immutable payment audit log (driver 3). Amount is a plain string to keep the wire
 * value exact across consumers.
 */
public record PaymentCapturedPayload(
        String eventId,
        String eventType,
        String processorRef,
        String travellerId,
        String amount,
        String currency,
        String idempotencyKey,
        String occurredAt) {

    public static final String EVENT_TYPE = "PaymentCaptured";
    public static final String TOPIC = "payment.captured";

    public static PaymentCapturedPayload from(PaymentDomainEvent.PaymentCaptured e) {
        return new PaymentCapturedPayload(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                e.processorRef(),
                e.traveller().value(),
                e.amount().amount().toPlainString(),
                e.amount().currency(),
                e.idempotencyKey(),
                e.occurredAt().toString());
    }
}
