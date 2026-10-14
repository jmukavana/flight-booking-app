package com.resilire.ingestion.application;

import com.resilire.ingestion.domain.feed.AlertSeverity;
import com.resilire.ingestion.domain.feed.WeatherAlertEvent;
import org.springframework.stereotype.Service;

/**
 * Derives a [0,1] disruption probability from a normalised weather alert.
 * The score feeds the resilience-score-service route forecaster: alerts at or
 * above {@link #IMMEDIATE_ACTION_THRESHOLD} trigger proactive recovery pre-staging.
 *
 * <p>Airport count amplifies systemic risk — a CRITICAL event over 6 hubs has
 * higher knock-on probability than a CRITICAL event over one regional airport.
 */
@Service
public class WeatherDisruptionRiskAssessor {

    static final double IMMEDIATE_ACTION_THRESHOLD = 0.85;

    private static final double[] BASELINE = {0.15, 0.40, 0.70, 0.92};

    public double assessRisk(WeatherAlertEvent alert) {
        double baseline = baselineFor(alert.severity());
        double multiplier = airportMultiplier(alert.affectedAirports().size());
        return Math.min(1.0, baseline * multiplier);
    }

    public boolean requiresImmediateAction(WeatherAlertEvent alert) {
        return assessRisk(alert) >= IMMEDIATE_ACTION_THRESHOLD;
    }

    private double baselineFor(AlertSeverity severity) {
        return switch (severity) {
            case LOW -> BASELINE[0];
            case MEDIUM -> BASELINE[1];
            case HIGH -> BASELINE[2];
            case CRITICAL -> BASELINE[3];
        };
    }

    private double airportMultiplier(int count) {
        if (count >= 5) return 1.15;
        if (count >= 3) return 1.08;
        return 1.0;
    }
}
