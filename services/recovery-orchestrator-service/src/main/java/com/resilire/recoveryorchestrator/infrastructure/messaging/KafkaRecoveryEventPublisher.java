package com.resilire.recoveryorchestrator.infrastructure.messaging;

import com.resilire.recoveryorchestrator.application.RecoveryEventPublisher;
import com.resilire.recoveryorchestrator.domain.RecoveryOptionType;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryCaseRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class KafkaRecoveryEventPublisher implements RecoveryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaRecoveryEventPublisher.class);

    private final OutboxJpaRepository outbox;
    private final ObjectMapper mapper;

    KafkaRecoveryEventPublisher(OutboxJpaRepository outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishCaseOpened(RecoveryCaseRecord c, List<RecoveryOptionType> options) {
        List<String> optionNames = options.stream().map(RecoveryOptionType::name).toList();
        RecoveryCaseOpenedPayload payload = new RecoveryCaseOpenedPayload(
                c.getId(), c.getDisruptionId(), c.getFlightId(),
                c.getSeverity(), optionNames, c.getOpenedAt());
        saveOutbox(RecoveryCaseOpenedPayload.TOPIC, c.getId(), payload);
    }

    private void saveOutbox(String topic, String aggregateId, Object payload) {
        try {
            OutboxRecord record = new OutboxRecord();
            record.setId(UUID.randomUUID().toString());
            record.setTopic(topic);
            record.setAggregateId(aggregateId);
            record.setPayload(mapper.writeValueAsString(payload));
            record.setOccurredAt(Instant.now());
            outbox.save(record);
        } catch (Exception e) {
            log.error("Failed to write outbox record for topic {}: {}", topic, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
