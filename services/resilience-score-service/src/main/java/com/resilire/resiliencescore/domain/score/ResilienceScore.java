package com.resilire.resiliencescore.domain.score;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A 0–100 resilience score with its explanation and provenance. Built from a model
 * {@link ModelPrediction} on the happy path; {@link #asLastKnown()} re-stamps a
 * cached model score when it is served during a model outage (NFR-ACC-1). Value object.
 */
public record ResilienceScore(
        int value,
        ScoreBand band,
        List<ScoreFactor> factors,
        String modelVersion,
        ScoreSource source,
        Instant computedAt) {

    public ResilienceScore {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("score must be in [0,100]");
        }
        Objects.requireNonNull(band, "band");
        Objects.requireNonNull(factors, "factors");
        Objects.requireNonNull(modelVersion, "modelVersion");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(computedAt, "computedAt");
        factors = List.copyOf(factors);
    }

    /** Translate a model prediction into a resilience score (higher = safer). */
    public static ResilienceScore fromModel(ModelPrediction prediction, Instant computedAt) {
        int value = (int) Math.round((1.0 - prediction.disruptionProbability()) * 100.0);
        return new ResilienceScore(value, ScoreBand.of(value), prediction.factors(),
                prediction.modelVersion(), ScoreSource.MODEL, computedAt);
    }

    /** Re-stamp this (cached) model score as stale last-known for an outage response. */
    public ResilienceScore asLastKnown() {
        return new ResilienceScore(value, band, factors, modelVersion, ScoreSource.LAST_KNOWN, computedAt);
    }
}
