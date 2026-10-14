package com.resilire.itinerary.infrastructure.messaging;

import com.resilire.itinerary.application.ItineraryProjectionService;
import com.resilire.itinerary.application.OrderCancelledProjection;
import com.resilire.itinerary.application.OrderPlacedProjection;
import com.resilire.itinerary.domain.itinerary.ItineraryId;
import com.resilire.itinerary.domain.itinerary.ItineraryLeg;
import com.resilire.itinerary.domain.itinerary.Money;
import com.resilire.itinerary.domain.itinerary.TravellerId;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Inbound adapter: consumes Booking's order events and drives the projection. The
 * consumer group ({@code spring.kafka.consumer.group-id}) and at-least-once
 * delivery mean a record may be redelivered; the projection is idempotent, so a
 * successful re-processing is a safe no-op. The Kafka offset commits only after the
 * listener returns, so a failure mid-projection is retried rather than lost.
 */
@Component
public class OrderEventListener {

    private final ItineraryProjectionService projection;
    private final ObjectMapper json;

    public OrderEventListener(ItineraryProjectionService projection, ObjectMapper json) {
        this.projection = projection;
        this.json = json;
    }

    @KafkaListener(topics = "order.created")
    public void onOrderCreated(String payload) {
        OrderCreatedEvent event = deserialize(payload, OrderCreatedEvent.class);
        ItineraryLeg leg = new ItineraryLeg(
                event.origin(),
                event.destination(),
                Instant.parse(event.departure()),
                Instant.parse(event.arrival()),
                event.provider(),
                event.pnr());
        projection.onOrderPlaced(new OrderPlacedProjection(
                ItineraryId.of(event.itineraryId()),
                TravellerId.of(event.travellerId()),
                leg,
                Money.of(event.amount(), event.currency()),
                Instant.parse(event.occurredAt())));
    }

    @KafkaListener(topics = "order.cancelled")
    public void onOrderCancelled(String payload) {
        OrderCancelledEvent event = deserialize(payload, OrderCancelledEvent.class);
        projection.onOrderCancelled(new OrderCancelledProjection(
                ItineraryId.of(event.itineraryId()),
                Instant.parse(event.occurredAt())));
    }

    private <T> T deserialize(String payload, Class<T> type) {
        try {
            return json.readValue(payload, type);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to deserialize " + type.getSimpleName(), e);
        }
    }
}
