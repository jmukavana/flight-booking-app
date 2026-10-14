package com.resilire.resiliencescore.application;

import com.resilire.resiliencescore.domain.score.ItineraryFeatures;
import com.resilire.resiliencescore.domain.score.ModelPrediction;
import com.resilire.resiliencescore.domain.score.ScoreModelUnavailableException;

/**
 * Anti-corruption port over ML Serving (the Python/FastAPI model, ADR-7). Keeps
 * the model behind the domain so it stays language-independent over HTTP and the
 * Spring core never embeds Python.
 */
public interface ScoreModelClient {

    /**
     * @throws ScoreModelUnavailableException if the model is down, times out, or
     *         returns low-confidence/no result — the caller then degrades safely.
     */
    ModelPrediction predict(ItineraryFeatures features);
}
