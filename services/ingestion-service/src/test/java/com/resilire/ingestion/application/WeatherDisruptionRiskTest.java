package com.resilire.ingestion.application;

import com.resilire.ingestion.domain.feed.AirportCode;
import com.resilire.ingestion.domain.feed.AlertSeverity;
import com.resilire.ingestion.domain.feed.WeatherAlertEvent;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherDisruptionRiskTest {

    private final WeatherDisruptionRiskAssessor assessor = new WeatherDisruptionRiskAssessor();

    @Test
    void low_severity_single_airport_stays_below_action_threshold() {
        WeatherAlertEvent alert = alert(AlertSeverity.LOW, "LHR");

        double risk = assessor.assessRisk(alert);

        assertThat(risk).isEqualTo(0.15);
        assertThat(assessor.requiresImmediateAction(alert)).isFalse();
    }

    @Test
    void critical_severity_triggers_immediate_action() {
        WeatherAlertEvent alert = alert(AlertSeverity.CRITICAL, "JFK");

        double risk = assessor.assessRisk(alert);

        assertThat(risk).isGreaterThanOrEqualTo(0.85);
        assertThat(assessor.requiresImmediateAction(alert)).isTrue();
    }

    @Test
    void five_or_more_airports_amplify_risk_by_1_15() {
        WeatherAlertEvent alert = alert(AlertSeverity.HIGH, "LHR", "CDG", "AMS", "FRA", "MAD");

        double risk = assessor.assessRisk(alert);

        assertThat(risk).isCloseTo(0.70 * 1.15, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void risk_is_capped_at_1_0_even_with_amplification() {
        WeatherAlertEvent alert = alert(AlertSeverity.CRITICAL, "JFK", "LHR", "CDG", "AMS", "FRA");

        double risk = assessor.assessRisk(alert);

        assertThat(risk).isLessThanOrEqualTo(1.0);
    }

    private WeatherAlertEvent alert(AlertSeverity severity, String... airports) {
        Set<AirportCode> codes = Set.of(airports).stream()
                .map(AirportCode::of)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new WeatherAlertEvent(
                "alert-1", severity, codes, "Test alert",
                Instant.now(), Instant.now().plusSeconds(3600),
                "test", Instant.now());
    }
}
