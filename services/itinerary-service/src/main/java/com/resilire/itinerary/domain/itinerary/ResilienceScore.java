package com.resilire.itinerary.domain.itinerary;

import java.time.Instant;
import java.util.Objects;

/**
 * The itinerary context's denormalised view of the resilience score (SDD §4:
 * itinerary carries {@code resilience_score}, {@code score_model_version}). Copied
 * from the {@code ScoreComputed} event owned by the Resilience Score Service — this
 * is the read model's local snapshot, not the authoritative score. {@code source}
 * carries the model/last-known/heuristic provenance so the UI can flag a degraded
 * score (NFR-ACC-1). Value object.
 */
public record ResilienceScore(int value, String band, String source, String modelVersion, Instant scoredAt) {

    public ResilienceScore {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("score must be in [0,100]");
        }
        Objects.requireNonNull(band, "band");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(modelVersion, "modelVersion");
        Objects.requireNonNull(scoredAt, "scoredAt");
    }
}
