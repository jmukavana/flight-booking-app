package com.resilire.resiliencescore.application;

import com.resilire.resiliencescore.domain.score.ResilienceScore;
import java.time.Instant;
import java.util.Objects;

/**
 * Application event: a score was (re)computed for a booked itinerary. The
 * messaging adapter turns this into the {@code ScoreComputed} integration event so
 * the Itinerary read model can attach the score (SDD §4: itinerary carries
 * {@code resilience_score}, {@code score_model_version}).
 */
public record ScoreComputed(String itineraryId, String travellerId, ResilienceScore score, Instant occurredAt) {

    public ScoreComputed {
        Objects.requireNonNull(itineraryId, "itineraryId");
        Objects.requireNonNull(travellerId, "travellerId");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
