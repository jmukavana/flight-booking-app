package com.resilire.resiliencescore.application;

/**
 * Outbound port for the {@code ScoreComputed} integration event (architecture.md
 * §6). The score lives only in Redis, so there is no transactional outbox here;
 * delivery is at-least-once and consumers are expected to be idempotent.
 */
public interface ScoreEventPublisher {

    void publish(ScoreComputed event);
}
