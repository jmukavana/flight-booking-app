package com.resilire.resiliencescore.application;

import com.resilire.resiliencescore.domain.score.HeuristicScorer;
import com.resilire.resiliencescore.domain.score.ItineraryFeatures;
import com.resilire.resiliencescore.domain.score.ItinerarySignature;
import com.resilire.resiliencescore.domain.score.ResilienceScore;
import com.resilire.resiliencescore.domain.score.ScoreCache;
import com.resilire.resiliencescore.domain.score.ScoreModelUnavailableException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Composes features, calls the model, and returns an explainable score (SDD §3).
 *
 * <p>Two entry points reflect the two flows of architecture.md §5.1:
 * <ul>
 *   <li>{@link #scoreCandidate} is the latency-sensitive point-of-sale path —
 *       cache-first so a repeat search never pays for the model again
 *       (driver 7, P95 ≤ 800 ms).</li>
 *   <li>{@link #scoreForOrder} is the async precompute on {@code OrderCreated} —
 *       it refreshes from the model and announces {@code ScoreComputed}.</li>
 * </ul>
 *
 * <p>Both degrade safely (NFR-ACC-1): if the model is unavailable, serve the cached
 * last-known score flagged stale, else a flagged heuristic — never a fabricated
 * model score.
 */
@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final ScoreModelClient model;
    private final ScoreCache cache;
    private final ScoreEventPublisher events;

    public ScoringService(ScoreModelClient model, ScoreCache cache, ScoreEventPublisher events) {
        this.model = model;
        this.cache = cache;
        this.events = events;
    }

    public ResilienceScore scoreCandidate(ItineraryFeatures features) {
        ItinerarySignature signature = ItinerarySignature.from(features);
        return cache.find(signature).orElseGet(() -> compute(features, signature));
    }

    public ResilienceScore scoreForOrder(ScoreForOrderCommand command) {
        ItinerarySignature signature = ItinerarySignature.from(command.features());
        ResilienceScore score = compute(command.features(), signature);
        events.publish(new ScoreComputed(
                command.itineraryId(), command.travellerId(), score, command.occurredAt()));
        return score;
    }

    /** Fresh model attempt with the degrade-safe fallback chain. Only model scores are cached. */
    private ResilienceScore compute(ItineraryFeatures features, ItinerarySignature signature) {
        try {
            ResilienceScore score = ResilienceScore.fromModel(model.predict(features), Instant.now());
            cache.put(signature, score);
            return score;
        } catch (ScoreModelUnavailableException e) {
            log.warn("Model unavailable for {}, degrading: {}", signature.value(), e.getMessage());
            return cache.find(signature)
                    .map(ResilienceScore::asLastKnown)
                    .orElseGet(() -> HeuristicScorer.score(features, Instant.now()));
        }
    }
}
