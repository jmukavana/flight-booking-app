package com.resilire.identityconsent.infrastructure.messaging;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drains the outbox to Kafka. At-least-once: an entry is marked published only
 * after the broker accepts it; a crash between sending and mark re-sends, which is
 * safe because downstream consumers are idempotent (architecture.md §7). Keyed
 * by the aggregate id (userId) to preserve per-Traveler ordering.
 */
@Component
public class OutboxRelay {

    private static final String TOPIC = "consent.changed";
    private static final Limit BATCH = Limit.of(100);

    private final OutboxJpaRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(OutboxJpaRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${resilire.outbox.relay-delay-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxRecord> pending =
                outbox.findByPublishedAtIsNullOrderByOccurredAtAsc(BATCH);
        for (OutboxRecord record : pending) {
            kafka.send(TOPIC, record.getAggregateId(), record.getPayload());
            record.markPublished(Instant.now());
        }
    }
}
