package com.resilire.payments.infrastructure.messaging;

import com.resilire.payments.application.IntegrationEventPublisher;
import com.resilire.payments.domain.payment.PaymentDomainEvent;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the payment integration event into the outbox table, inside the application
 * service's transaction, so the ledger entry and its announcement commit atomically
 * (ADR-10). The Kafka key is the processor ref, keeping a payment's events ordered
 * per topic. Actual delivery is the relay's job.
 */
@Component
public class OutboxIntegrationEventPublisher implements IntegrationEventPublisher {

    private static final String AGGREGATE_TYPE = "PaymentLedger";

    private final OutboxJpaRepository outbox;
    private final ObjectMapper json;

    public OutboxIntegrationEventPublisher(OutboxJpaRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    public void publish(PaymentDomainEvent event) {
        OutboxRecord record = switch (event) {
            case PaymentDomainEvent.PaymentCaptured captured -> toRecord(captured.processorRef(),
                    PaymentCapturedPayload.EVENT_TYPE, PaymentCapturedPayload.TOPIC,
                    serialize(PaymentCapturedPayload.from(captured)), captured.occurredAt());
            case PaymentDomainEvent.PaymentRefunded refunded -> toRecord(refunded.processorRef(),
                    PaymentRefundedPayload.EVENT_TYPE, PaymentRefundedPayload.TOPIC,
                    serialize(PaymentRefundedPayload.from(refunded)), refunded.occurredAt());
        };
        outbox.save(record);
    }

    private OutboxRecord toRecord(String aggregateId, String eventType, String topic, String payload,
                                  java.time.Instant occurredAt) {
        return new OutboxRecord(AGGREGATE_TYPE, aggregateId, eventType, topic, payload, occurredAt);
    }

    private String serialize(Object payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize payment event", e);
        }
    }
}
