package com.resilire.resiliencescore.infrastructure.messaging;

import com.resilire.resiliencescore.application.ScoreForOrderCommand;
import com.resilire.resiliencescore.application.ScoringService;
import com.resilire.resiliencescore.domain.score.ItineraryFeatures;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Inbound adapter: consumes Booking's {@code OrderCreated} and precomputes the
 * score for the booked itinerary (the async precompute of architecture.md §5.1 /
 * driver 7), then announces {@code ScoreComputed}. Idempotent at the domain level —
 * scoring the same signature is deterministic and cache-backed — so at-least-once
 * redelivery is safe. A single leg means zero connections in Phase 1.
 */
@Component
public class OrderEventListener {

    private final ScoringService scoring;
    private final ObjectMapper json;

    public OrderEventListener(ScoringService scoring, ObjectMapper json) {
        this.scoring = scoring;
        this.json = json;
    }

    @KafkaListener(topics = "order.created")
    public void onOrderCreated(String payload) {
        OrderCreatedEvent event = deserialize(payload);
        ItineraryFeatures features = new ItineraryFeatures(
                event.origin(),
                event.destination(),
                event.provider(),
                Instant.parse(event.departure()),
                0);
        scoring.scoreForOrder(new ScoreForOrderCommand(
                event.itineraryId(),
                event.travellerId(),
                features,
                Instant.parse(event.occurredAt())));
    }

    private OrderCreatedEvent deserialize(String payload) {
        try {
            return json.readValue(payload, OrderCreatedEvent.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to deserialize OrderCreated", e);
        }
    }
}
