package com.resilire.resiliencescore.domain.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RouteForecastTest {

    @Test
    void observed_route_returns_high_confidence_and_specific_otp() {
        ForecastResult result = RouteForecaster.forecast("NBO", "LHR", "KQ");

        assertThat(result.otpRate()).isCloseTo(0.78, within(0.001));
        assertThat(result.disruptionProbability()).isCloseTo(0.22, within(0.001));
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.80);
    }

    @Test
    void unknown_route_returns_default_otp_and_lower_confidence() {
        ForecastResult result = RouteForecaster.forecast("AAA", "BBB", "XX");

        assertThat(result.otpRate()).isCloseTo(0.80, within(0.001));
        assertThat(result.confidence()).isLessThan(0.75);
    }

    @Test
    void route_lookup_is_case_insensitive() {
        ForecastResult lower = RouteForecaster.forecast("nbo", "lhr", "kq");
        ForecastResult upper = RouteForecaster.forecast("NBO", "LHR", "KQ");

        assertThat(lower.disruptionProbability()).isEqualTo(upper.disruptionProbability());
    }

    @Test
    void heuristic_separate_ticket_penalty_reduces_score() {
        Instant now = java.time.Instant.parse("2026-09-01T10:00:00Z");
        ItineraryFeatures withSeparate = new ItineraryFeatures("NBO", "LHR", "KQ", now, 1, true);
        ItineraryFeatures withoutSeparate = new ItineraryFeatures("NBO", "LHR", "KQ", now, 1, false);

        ResilienceScore scoreWith = HeuristicScorer.score(withSeparate, now);
        ResilienceScore scoreWithout = HeuristicScorer.score(withoutSeparate, now);

        assertThat(scoreWith.value()).isLessThan(scoreWithout.value());
        assertThat(scoreWith.factors()).anyMatch(f -> "SEPARATE_TICKET".equals(f.code()));
    }

    @Test
    void forecast_result_validates_probability_bounds() {
        ForecastResult result = RouteForecaster.forecast("DXB", "LHR", "EK");

        assertThat(result.disruptionProbability()).isBetween(0.0, 1.0);
        assertThat(result.otpRate()).isBetween(0.0, 1.0);
        assertThat(result.confidence()).isBetween(0.0, 1.0);
    }
}
