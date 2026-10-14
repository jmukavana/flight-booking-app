package com.resilire.resiliencescore.domain.score;

/**
 * Route-level disruption forecast (Phase 3 AI depth — architecture.md §17).
 * {@code confidence} reflects data coverage: 0.85 for routes with observed OTP
 * history, 0.60 for the synthetic default. Both are labelled so the UI can
 * distinguish a data-backed forecast from the fallback.
 */
public record ForecastResult(
        String origin,
        String destination,
        String carrier,
        double disruptionProbability,
        double otpRate,
        double confidence) {

    public ForecastResult {
        if (disruptionProbability < 0 || disruptionProbability > 1) {
            throw new IllegalArgumentException("disruptionProbability must be in [0,1]");
        }
        if (otpRate < 0 || otpRate > 1) {
            throw new IllegalArgumentException("otpRate must be in [0,1]");
        }
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be in [0,1]");
        }
    }
}
