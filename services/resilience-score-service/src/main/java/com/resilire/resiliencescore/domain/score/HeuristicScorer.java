package com.resilire.resiliencescore.domain.score;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic fallback used only when ML Serving is unavailable and no prior
 * model score is cached. It is explicitly labelled {@link ScoreSource#HEURISTIC}
 * so it is never mistaken for a model output (NFR-ACC-1 sanctions "fall back to
 * last-known/heuristic" — provided the degraded provenance is flagged).
 *
 * <p>The rule is intentionally simple and explainable: start resilient, penalise
 * each connection (the dominant misconnection driver) and red-eye departures.
 */
public final class HeuristicScorer {

    public static final String MODEL_VERSION = "heuristic-v1";

    private static final int BASE = 90;
    private static final int CONNECTION_PENALTY = 18;
    private static final int RED_EYE_PENALTY = 8;
    private static final int SEPARATE_TICKET_PENALTY = 15;

    private HeuristicScorer() {
    }

    public static ResilienceScore score(ItineraryFeatures features, Instant computedAt) {
        List<ScoreFactor> factors = new ArrayList<>();
        int value = BASE;

        int connectionHit = features.connections() * CONNECTION_PENALTY;
        if (connectionHit > 0) {
            value -= connectionHit;
            factors.add(new ScoreFactor("CONNECTIONS",
                    features.connections() + " connection(s) raise misconnection risk", -connectionHit));
        } else {
            factors.add(new ScoreFactor("NONSTOP", "Non-stop itinerary, no misconnection risk", 0));
        }

        int departureHour = features.departure().atZone(java.time.ZoneOffset.UTC).getHour();
        if (departureHour < 6) {
            value -= RED_EYE_PENALTY;
            factors.add(new ScoreFactor("RED_EYE", "Early-morning departure is more cancellation-prone",
                    -RED_EYE_PENALTY));
        }

        if (features.separateTickets()) {
            value -= SEPARATE_TICKET_PENALTY;
            factors.add(new ScoreFactor("SEPARATE_TICKET",
                    "Legs on separate tickets — no carrier misconnection protection",
                    -SEPARATE_TICKET_PENALTY));
        }

        value = Math.max(0, Math.min(100, value));
        return new ResilienceScore(value, ScoreBand.of(value), List.copyOf(factors),
                MODEL_VERSION, ScoreSource.HEURISTIC, computedAt);
    }
}
