package com.resilire.disruptiondetection.infrastructure.messaging;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OutboxRelay {

    private static final Limit BATCH = Limit.of(100);

    private final OutboxJpaRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    OutboxRelay(OutboxJpaRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${resilire.outbox.relay-delay-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxRecord> pending =
                outbox.findByPublishedAtIsNullOrderByOccurredAtAsc(BATCH);
        for (OutboxRecord record : pending) {
            kafka.send(record.getTopic(), record.getAggregateId(), record.getPayload());
            record.markPublished(Instant.now());
        }
    }
}
