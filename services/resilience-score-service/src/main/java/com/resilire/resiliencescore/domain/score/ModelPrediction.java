package com.resilire.resiliencescore.domain.score;

import java.util.List;
import java.util.Objects;

/**
 * Raw output of ML Serving's misconnection/cancellation model (architecture.md
 * §5.1): a disruption probability in [0,1] plus the factors behind it and the
 * model version that produced them. The domain turns this into a 0–100
 * {@link ResilienceScore}. Value object.
 */
public record ModelPrediction(double disruptionProbability, List<ScoreFactor> factors, String modelVersion) {

    public ModelPrediction {
        Objects.requireNonNull(factors, "factors");
        Objects.requireNonNull(modelVersion, "modelVersion");
        if (disruptionProbability < 0.0 || disruptionProbability > 1.0) {
            throw new IllegalArgumentException("disruptionProbability must be in [0,1]");
        }
        factors = List.copyOf(factors);
    }
}
