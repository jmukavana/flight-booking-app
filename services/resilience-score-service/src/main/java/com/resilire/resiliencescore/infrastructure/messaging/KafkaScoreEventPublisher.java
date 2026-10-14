package com.resilire.resiliencescore.infrastructure.messaging;

import com.resilire.resiliencescore.application.ScoreComputed;
import com.resilire.resiliencescore.application.ScoreEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Publishes {@code ScoreComputed} to Kafka, keyed by itineraryId so a Traveller's
 * score updates stay ordered per partition. The score is cache-only state, so
 * there is no transactional outbox; delivery is at-least-once and consumers are
 * idempotent.
 */
@Component
class KafkaScoreEventPublisher implements ScoreEventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    KafkaScoreEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    @Override
    public void publish(ScoreComputed event) {
        ScoreComputedPayload payload = ScoreComputedPayload.from(event);
        kafka.send(ScoreComputedPayload.TOPIC, payload.itineraryId(), serialize(payload));
    }

    private String serialize(ScoreComputedPayload payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize ScoreComputed event", e);
        }
    }
}
