package com.resilire.recoveryorchestrator.infrastructure.messaging;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxJpaRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final long relayDelayMs;

    OutboxRelay(OutboxJpaRepository outbox, KafkaTemplate<String, String> kafka,
                @Value("${resilire.outbox.relay-delay-ms:2000}") long relayDelayMs) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.relayDelayMs = relayDelayMs;
    }

    @Scheduled(fixedDelayString = "${resilire.outbox.relay-delay-ms:2000}")
    @Transactional
    public void relay() {
        List<OutboxRecord> pending = outbox.findTop50ByPublishedAtIsNullOrderByOccurredAtAsc();
        for (OutboxRecord record : pending) {
            try {
                kafka.send(record.getTopic(), record.getAggregateId(), record.getPayload()).get();
                record.markPublished(Instant.now());
                outbox.save(record);
            } catch (Exception e) {
                log.warn("Outbox relay failed for {}: {}", record.getId(), e.getMessage());
            }
        }
    }
}
