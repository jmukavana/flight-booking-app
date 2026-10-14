package com.resilire.identityconsent.infrastructure.messaging;

import com.resilire.identityconsent.application.IntegrationEventPublisher;
import com.resilire.identityconsent.domain.consent.ConsentDomainEvent;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the {@code ConsentChanged} event into the outbox table. Invoked inside
 * the application service's transaction, so the event row commits atomically
 * with the consent entry (ADR-10). Actual Kafka delivery is the relay's job.
 */
@Component
public class OutboxIntegrationEventPublisher implements IntegrationEventPublisher {

    private static final String AGGREGATE_TYPE = "ConsentLedger";

    private final OutboxJpaRepository outbox;
    private final ObjectMapper json;

    public OutboxIntegrationEventPublisher(OutboxJpaRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    public void publish(ConsentDomainEvent event) {
        ConsentChangedPayload payload = ConsentChangedPayload.from(event);
        outbox.save(new OutboxRecord(
                AGGREGATE_TYPE,
                payload.userId(),
                ConsentChangedPayload.EVENT_TYPE,
                serialize(payload),
                event.occurredAt()));
    }

    private String serialize(ConsentChangedPayload payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize ConsentChanged event", e);
        }
    }
}
