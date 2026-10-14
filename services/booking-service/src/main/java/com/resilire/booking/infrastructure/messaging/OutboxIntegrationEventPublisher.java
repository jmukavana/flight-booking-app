package com.resilire.booking.infrastructure.messaging;

import com.resilire.booking.application.IntegrationEventPublisher;
import com.resilire.booking.domain.order.OrderDomainEvent;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the integration event into the outbox table. Invoked inside the
 * application service's transaction, so the event row commits atomically with the
 * order (ADR-10). Actual Kafka delivery is the relay's job. The Kafka key is the
 * itineraryId, so all events about one itinerary stay ordered per topic.
 */
@Component
public class OutboxIntegrationEventPublisher implements IntegrationEventPublisher {

    private static final String AGGREGATE_TYPE = "Order";

    private final OutboxJpaRepository outbox;
    private final ObjectMapper json;

    public OutboxIntegrationEventPublisher(OutboxJpaRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    public void publish(OrderDomainEvent event) {
        OutboxRecord record = switch (event) {
            case OrderDomainEvent.OrderPlaced placed -> toRecord(event,
                    OrderCreatedPayload.EVENT_TYPE, OrderCreatedPayload.TOPIC,
                    serialize(OrderCreatedPayload.from(placed)));
            case OrderDomainEvent.OrderCancelled cancelled -> toRecord(event,
                    OrderCancelledPayload.EVENT_TYPE, OrderCancelledPayload.TOPIC,
                    serialize(OrderCancelledPayload.from(cancelled)));
        };
        outbox.save(record);
    }

    private OutboxRecord toRecord(OrderDomainEvent event, String eventType, String topic, String payload) {
        return new OutboxRecord(AGGREGATE_TYPE, event.itineraryId().value().toString(),
                eventType, topic, payload, event.occurredAt());
    }

    private String serialize(Object payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize integration event", e);
        }
    }
}
