package com.resilire.itinerary.infrastructure.messaging;

import com.resilire.itinerary.application.ItineraryProjectionService;
import com.resilire.itinerary.application.ScoreComputedProjection;
import com.resilire.itinerary.domain.itinerary.ItineraryId;
import com.resilire.itinerary.domain.itinerary.ResilienceScore;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Inbound adapter: consumes {@code ScoreComputed} and attaches the score to the
 * itinerary read model (SDD §4). Idempotent and reorder-tolerant via the
 * projection's own score high-water mark, so at-least-once redelivery is safe.
 */
@Component
public class ScoreEventListener {

    private final ItineraryProjectionService projection;
    private final ObjectMapper json;

    public ScoreEventListener(ItineraryProjectionService projection, ObjectMapper json) {
        this.projection = projection;
        this.json = json;
    }

    @KafkaListener(topics = "score.computed")
    public void onScoreComputed(String payload) {
        ScoreComputedEvent event = deserialize(payload);
        ResilienceScore score = new ResilienceScore(
                event.score(),
                event.band(),
                event.source(),
                event.modelVersion(),
                Instant.parse(event.computedAt()));
        projection.onScoreComputed(new ScoreComputedProjection(
                ItineraryId.of(event.itineraryId()), score));
    }

    private ScoreComputedEvent deserialize(String payload) {
        try {
            return json.readValue(payload, ScoreComputedEvent.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to deserialize ScoreComputed", e);
        }
    }
}
