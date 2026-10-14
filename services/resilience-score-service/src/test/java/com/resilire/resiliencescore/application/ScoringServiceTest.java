package com.resilire.resiliencescore.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.resilire.resiliencescore.domain.score.ItineraryFeatures;
import com.resilire.resiliencescore.domain.score.ItinerarySignature;
import com.resilire.resiliencescore.domain.score.ModelPrediction;
import com.resilire.resiliencescore.domain.score.ResilienceScore;
import com.resilire.resiliencescore.domain.score.ScoreBand;
import com.resilire.resiliencescore.domain.score.ScoreCache;
import com.resilire.resiliencescore.domain.score.ScoreFactor;
import com.resilire.resiliencescore.domain.score.ScoreModelUnavailableException;
import com.resilire.resiliencescore.domain.score.ScoreSource;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Scoring flows: cache-first point-of-sale path and degrade-safe fallback chain. */
class ScoringServiceTest {

    private final ItineraryFeatures features =
            new ItineraryFeatures("LHR", "JFK", "BA", Instant.parse("2026-07-01T10:00:00Z"), 1);
    private final ItinerarySignature signature = ItinerarySignature.from(features);
    private final Instant occurredAt = Instant.parse("2026-07-01T08:00:00Z");

    private FakeModel model;
    private FakeCache cache;
    private FakePublisher publisher;
    private ScoringService service;

    @BeforeEach
    void setUp() {
        model = new FakeModel();
        cache = new FakeCache();
        publisher = new FakePublisher();
        service = new ScoringService(model, cache, publisher);
    }

    @Test
    void scoreCandidate_calls_model_and_caches_on_miss() {
        model.next = prediction(0.10);

        ResilienceScore score = service.scoreCandidate(features);

        assertThat(score.value()).isEqualTo(90);
        assertThat(score.source()).isEqualTo(ScoreSource.MODEL);
        assertThat(cache.find(signature)).isPresent();
    }

    @Test
    void scoreCandidate_returns_cached_score_without_calling_model() {
        cache.put(signature, modelScore(70));
        model.fail = true; // would throw if called

        ResilienceScore score = service.scoreCandidate(features);

        assertThat(model.calls).isZero();
        assertThat(score.value()).isEqualTo(70);
    }

    @Test
    void scoreCandidate_degrades_to_heuristic_when_model_down_and_cache_empty() {
        model.fail = true;

        ResilienceScore score = service.scoreCandidate(features);

        assertThat(score.source()).isEqualTo(ScoreSource.HEURISTIC);
    }

    @Test
    void scoreForOrder_returns_model_score_and_publishes_event() {
        model.next = prediction(0.20);

        ResilienceScore score = service.scoreForOrder(command());

        assertThat(score.value()).isEqualTo(80);
        assertThat(score.source()).isEqualTo(ScoreSource.MODEL);
        assertThat(publisher.last).isNotNull();
        assertThat(publisher.last.itineraryId()).isEqualTo("itin-1");
        assertThat(publisher.last.score().value()).isEqualTo(80);
    }

    @Test
    void scoreForOrder_serves_last_known_when_model_down() {
        cache.put(signature, modelScore(85));
        model.fail = true;

        ResilienceScore score = service.scoreForOrder(command());

        assertThat(score.source()).isEqualTo(ScoreSource.LAST_KNOWN);
        assertThat(score.value()).isEqualTo(85);
        assertThat(publisher.last.score().source()).isEqualTo(ScoreSource.LAST_KNOWN);
    }

    private ScoreForOrderCommand command() {
        return new ScoreForOrderCommand("itin-1", "traveller-1", features, occurredAt);
    }

    private ModelPrediction prediction(double probability) {
        return new ModelPrediction(probability, List.of(new ScoreFactor("F", "factor", -1)), "model-v1");
    }

    private ResilienceScore modelScore(int value) {
        return new ResilienceScore(value, ScoreBand.of(value), List.of(), "model-v1",
                ScoreSource.MODEL, Instant.parse("2026-07-01T07:00:00Z"));
    }

    private static final class FakeModel implements ScoreModelClient {
        ModelPrediction next;
        boolean fail;
        int calls;

        @Override
        public ModelPrediction predict(ItineraryFeatures features) {
            calls++;
            if (fail) {
                throw new ScoreModelUnavailableException("test");
            }
            return next;
        }
    }

    private static final class FakeCache implements ScoreCache {
        private final Map<String, ResilienceScore> store = new HashMap<>();

        @Override
        public Optional<ResilienceScore> find(ItinerarySignature signature) {
            return Optional.ofNullable(store.get(signature.value()));
        }

        @Override
        public void put(ItinerarySignature signature, ResilienceScore score) {
            store.put(signature.value(), score);
        }
    }

    private static final class FakePublisher implements ScoreEventPublisher {
        ScoreComputed last;

        @Override
        public void publish(ScoreComputed event) {
            last = event;
        }
    }
}
