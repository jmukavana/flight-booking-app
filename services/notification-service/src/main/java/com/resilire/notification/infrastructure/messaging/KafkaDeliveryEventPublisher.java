package com.resilire.notification.infrastructure.messaging;

import com.resilire.notification.application.DeliveryEventPublisher;
import com.resilire.notification.domain.notification.DeliveryResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Publishes {@code NotificationDelivered} to Kafka, keyed by travellerId so a
 * Traveler's delivery log stays ordered per partition. At-least-once; the score
 * lives only in the event stream, so there is no transactional outbox here.
 */
@Component
class KafkaDeliveryEventPublisher implements DeliveryEventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    KafkaDeliveryEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    @Override
    public void publish(DeliveryResult result) {
        NotificationDeliveredPayload payload = NotificationDeliveredPayload.from(result);
        kafka.send(NotificationDeliveredPayload.TOPIC, payload.travellerId(), serialize(payload));
    }

    private String serialize(NotificationDeliveredPayload payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize NotificationDelivered event", e);
        }
    }
}
