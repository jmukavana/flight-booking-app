package com.resilire.resiliencescore.domain.score;

import java.util.Locale;
import java.util.Map;

/**
 * Phase 3 route-level disruption forecaster (architecture.md §10 / FR-2 depth).
 *
 * <p>Uses a static OTP table seeded from open-data observations. Routes not in the
 * table fall back to the industry-average 80% OTP with lower confidence (0.60 vs
 * 0.85). Phase 4 replaces the static map with an ML model trained on live OTP feeds.
 *
 * <p>OTP (on-time performance) is defined as arrival within 15 min of scheduled time.
 * Disruption probability = 1 − OTP rate.
 */
public final class RouteForecaster {

    private static final double DEFAULT_OTP = 0.80;
    private static final double OBSERVED_CONFIDENCE = 0.85;
    private static final double DEFAULT_CONFIDENCE = 0.60;

    private static final Map<String, Double> OTP_BY_ROUTE = Map.ofEntries(
            Map.entry("NBO-LHR-KQ", 0.78),
            Map.entry("LHR-NBO-KQ", 0.77),
            Map.entry("DXB-LHR-EK", 0.86),
            Map.entry("LHR-DXB-EK", 0.87),
            Map.entry("JFK-LHR-BA", 0.83),
            Map.entry("LHR-JFK-BA", 0.82),
            Map.entry("NBO-MBA-KQ", 0.91),
            Map.entry("MBA-NBO-KQ", 0.90),
            Map.entry("CDG-LHR-AF", 0.89),
            Map.entry("LHR-CDG-AF", 0.88),
            Map.entry("ORD-LHR-AA", 0.80),
            Map.entry("LHR-ORD-AA", 0.81),
            Map.entry("SYD-DXB-EK", 0.84),
            Map.entry("JNB-LHR-SA", 0.76)
    );

    private RouteForecaster() {}

    public static ForecastResult forecast(String origin, String destination, String carrier) {
        String key = normalise(origin) + "-" + normalise(destination) + "-" + normalise(carrier);
        boolean observed = OTP_BY_ROUTE.containsKey(key);
        double otpRate = OTP_BY_ROUTE.getOrDefault(key, DEFAULT_OTP);
        return new ForecastResult(
                normalise(origin), normalise(destination), normalise(carrier),
                1.0 - otpRate, otpRate,
                observed ? OBSERVED_CONFIDENCE : DEFAULT_CONFIDENCE);
    }

    private static String normalise(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}
