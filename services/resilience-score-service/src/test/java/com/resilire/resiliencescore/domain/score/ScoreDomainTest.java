package com.resilire.resiliencescore.domain.score;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Score translation + banding + heuristic — pure domain, no Spring, fast. */
class ScoreDomainTest {

    private final Instant now = Instant.parse("2026-07-01T10:00:00Z");

    @Test
    void fromModel_maps_disruption_probability_to_a_resilience_score() {
        ModelPrediction prediction = new ModelPrediction(0.25,
                List.of(new ScoreFactor("WX", "weather", -10)), "model-v1");

        ResilienceScore score = ResilienceScore.fromModel(prediction, now);

        assertThat(score.value()).isEqualTo(75);
        assertThat(score.band()).isEqualTo(ScoreBand.RESILIENT);
        assertThat(score.source()).isEqualTo(ScoreSource.MODEL);
        assertThat(score.modelVersion()).isEqualTo("model-v1");
    }

    @Test
    void band_boundaries() {
        assertThat(ScoreBand.of(49)).isEqualTo(ScoreBand.AT_RISK);
        assertThat(ScoreBand.of(50)).isEqualTo(ScoreBand.MODERATE);
        assertThat(ScoreBand.of(74)).isEqualTo(ScoreBand.MODERATE);
        assertThat(ScoreBand.of(75)).isEqualTo(ScoreBand.RESILIENT);
    }

    @Test
    void asLastKnown_only_changes_the_provenance() {
        ResilienceScore model = ResilienceScore.fromModel(
                new ModelPrediction(0.1, List.of(), "model-v1"), now);

        ResilienceScore stale = model.asLastKnown();

        assertThat(stale.source()).isEqualTo(ScoreSource.LAST_KNOWN);
        assertThat(stale.value()).isEqualTo(model.value());
        assertThat(stale.computedAt()).isEqualTo(model.computedAt());
    }

    @Test
    void heuristic_penalises_each_connection_and_flags_its_provenance() {
        ItineraryFeatures twoStops = new ItineraryFeatures("LHR", "SYD", "QF", now, 2);

        ResilienceScore score = HeuristicScorer.score(twoStops, now);

        assertThat(score.value()).isEqualTo(54); // 90 - 2*18
        assertThat(score.source()).isEqualTo(ScoreSource.HEURISTIC);
        assertThat(score.modelVersion()).isEqualTo(HeuristicScorer.MODEL_VERSION);
    }
}
