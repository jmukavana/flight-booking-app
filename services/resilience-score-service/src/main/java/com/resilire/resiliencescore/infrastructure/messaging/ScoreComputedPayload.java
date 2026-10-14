package com.resilire.resiliencescore.infrastructure.messaging;

import com.resilire.resiliencescore.application.ScoreComputed;
import com.resilire.resiliencescore.domain.score.ResilienceScore;
import java.util.UUID;

/**
 * Published language for the {@code ScoreComputed} integration event (architecture.md
 * §6). The Itinerary read model attaches {@code score}/{@code modelVersion}; the
 * {@code source} flag tells consumers whether it is a live model score or a
 * degraded one (NFR-ACC-1).
 */
public record ScoreComputedPayload(
        String eventId,
        String eventType,
        String itineraryId,
        String travellerId,
        int score,
        String band,
        String source,
        String modelVersion,
        String computedAt) {

    public static final String EVENT_TYPE = "ScoreComputed";
    public static final String TOPIC = "score.computed";

    public static ScoreComputedPayload from(ScoreComputed event) {
        ResilienceScore score = event.score();
        return new ScoreComputedPayload(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                event.itineraryId(),
                event.travellerId(),
                score.value(),
                score.band().name(),
                score.source().name(),
                score.modelVersion(),
                score.computedAt().toString());
    }
}
