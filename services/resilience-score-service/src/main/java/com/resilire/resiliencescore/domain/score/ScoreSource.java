package com.resilire.resiliencescore.domain.score;

/**
 * Provenance of a score — surfaced to the caller so a degraded score is never
 * mistaken for a live model output (NFR-ACC-1: flag, never fabricate).
 *
 * <ul>
 *   <li>{@code MODEL} — fresh prediction from ML Serving.</li>
 *   <li>{@code LAST_KNOWN} — a previously cached model score, served because the
 *       model was unavailable on this request (stale-flagged).</li>
 *   <li>{@code HEURISTIC} — deterministic fallback when no model score exists.</li>
 * </ul>
 */
public enum ScoreSource {
    MODEL,
    LAST_KNOWN,
    HEURISTIC
}
