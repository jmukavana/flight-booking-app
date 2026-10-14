package com.resilire.resiliencescore.infrastructure.model;

import com.resilire.resiliencescore.domain.score.ModelPrediction;
import com.resilire.resiliencescore.domain.score.ScoreFactor;
import java.util.List;

/** Response body from ML Serving's {@code /v1/score} endpoint, mapped to the domain at the boundary. */
record MlScoreResponse(double disruptionProbability, List<Factor> factors, String modelVersion) {

    ModelPrediction toPrediction() {
        List<ScoreFactor> mapped = factors == null ? List.of()
                : factors.stream().map(Factor::toDomain).toList();
        return new ModelPrediction(disruptionProbability, mapped, modelVersion);
    }

    record Factor(String code, String description, double weight) {
        ScoreFactor toDomain() {
            return new ScoreFactor(code, description, weight);
        }
    }
}
