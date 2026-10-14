package com.resilire.resiliencescore.infrastructure.model;

import com.resilire.resiliencescore.domain.score.ItineraryFeatures;

/** Request body for ML Serving's {@code /v1/score} endpoint. */
record MlScoreRequest(String origin, String destination, String marketingCarrier,
                      String departure, int connections) {

    static MlScoreRequest from(ItineraryFeatures features) {
        return new MlScoreRequest(features.origin(), features.destination(),
                features.marketingCarrier(), features.departure().toString(), features.connections());
    }
}
