package com.resilire.resiliencescore.domain.score;

/**
 * Human-facing banding of the 0–100 resilience score. Higher score = more
 * resilient (lower predicted disruption risk).
 */
public enum ScoreBand {
    AT_RISK,
    MODERATE,
    RESILIENT;

    public static ScoreBand of(int score) {
        if (score < 50) {
            return AT_RISK;
        }
        if (score < 75) {
            return MODERATE;
        }
        return RESILIENT;
    }
}
