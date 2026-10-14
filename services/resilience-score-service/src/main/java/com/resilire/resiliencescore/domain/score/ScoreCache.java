package com.resilire.resiliencescore.domain.score;

import java.util.Optional;

/**
 * Outbound port owned by the domain, implemented by a Redis adapter. The cache is
 * the score's only store — the service is otherwise stateless (architecture.md §6).
 * Entries expire by TTL so a stale score is eventually recomputed.
 */
public interface ScoreCache {

    Optional<ResilienceScore> find(ItinerarySignature signature);

    void put(ItinerarySignature signature, ResilienceScore score);
}
