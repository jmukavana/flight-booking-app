package com.resilire.resiliencescore.domain.score;

/**
 * Raised by the model port when ML Serving cannot produce a prediction (down,
 * timed out, or low-confidence). It triggers the degrade-safe path — never a
 * fabricated score (NFR-ACC-1).
 */
public class ScoreModelUnavailableException extends RuntimeException {

    public ScoreModelUnavailableException(String reason) {
        super("Score model unavailable: " + reason);
    }
}
