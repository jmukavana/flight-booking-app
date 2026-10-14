package com.resilire.disruptiondetection.infrastructure.messaging;

import com.resilire.disruptiondetection.application.DisruptionEventPublisher;
import com.resilire.disruptiondetection.domain.disruption.DisruptionDomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class OutboxDisruptionEventPublisher implements DisruptionEventPublisher {

    private final OutboxJpaRepository outbox;
    private final ObjectMapper json;

    OutboxDisruptionEventPublisher(OutboxJpaRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    public void publish(DisruptionDomainEvent event) {
        OutboxRecord record = new OutboxRecord();
        record.setId(UUID.randomUUID().toString());
        record.setOccurredAt(Instant.now());

        switch (event) {
            case DisruptionDomainEvent.DisruptionDetected d -> {
                DisruptionDetectedPayload payload = DisruptionDetectedPayload.from(d);
                record.setTopic(DisruptionDetectedPayload.TOPIC);
                record.setAggregateId(d.flight().flightId());
                record.setPayload(serialize(payload));
            }
            case DisruptionDomainEvent.DisruptionResolved r -> {
                DisruptionResolvedPayload payload = DisruptionResolvedPayload.from(r);
                record.setTopic(DisruptionResolvedPayload.TOPIC);
                record.setAggregateId(r.flight().flightId());
                record.setPayload(serialize(payload));
            }
        }
        outbox.save(record);
    }

    private String serialize(Object payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize disruption event", e);
        }
    }
}
