package com.resilire.resiliencescore.infrastructure.model;

import com.resilire.resiliencescore.application.ScoreModelClient;
import com.resilire.resiliencescore.domain.score.ItineraryFeatures;
import com.resilire.resiliencescore.domain.score.ModelPrediction;
import com.resilire.resiliencescore.domain.score.ScoreModelUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Anti-corruption adapter over ML Serving (ADR-7). Any transport or protocol
 * failure — connection refused, timeout, 4xx/5xx, empty body — collapses to
 * {@link ScoreModelUnavailableException} so the application's degrade-safe path
 * (last-known/heuristic) takes over rather than surfacing an error to the buyer.
 */
@Component
class HttpScoreModelClient implements ScoreModelClient {

    private final RestClient mlServing;

    HttpScoreModelClient(RestClient mlServingRestClient) {
        this.mlServing = mlServingRestClient;
    }

    @Override
    public ModelPrediction predict(ItineraryFeatures features) {
        try {
            MlScoreResponse response = mlServing.post()
                    .uri("/v1/score")
                    .body(MlScoreRequest.from(features))
                    .retrieve()
                    .body(MlScoreResponse.class);
            if (response == null) {
                throw new ScoreModelUnavailableException("empty response from ML serving");
            }
            return response.toPrediction();
        } catch (RestClientException e) {
            throw new ScoreModelUnavailableException(e.getMessage());
        }
    }
}
