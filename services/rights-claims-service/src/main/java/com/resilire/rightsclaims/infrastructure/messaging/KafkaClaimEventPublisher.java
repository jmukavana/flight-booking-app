package com.resilire.rightsclaims.infrastructure.messaging;

import com.resilire.rightsclaims.application.ClaimEventPublisher;
import com.resilire.rightsclaims.infrastructure.persistence.ClaimRecord;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class KafkaClaimEventPublisher implements ClaimEventPublisher {

    private static final String TOPIC_SUBMITTED = "claim.submitted";
    private static final String TOPIC_APPROVED = "claim.approved";

    private final OutboxJpaRepository outbox;
    private final ObjectMapper json;

    KafkaClaimEventPublisher(OutboxJpaRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    public void publishClaimSubmitted(ClaimRecord claim) {
        append(TOPIC_SUBMITTED, claim.getId(),
                new ClaimPayload(UUID.randomUUID().toString(), "ClaimSubmitted",
                        claim.getId(), claim.getTravellerId(), claim.getAssessmentId(),
                        claim.getStatus().name(), claim.getSubmittedAt().toString()));
    }

    @Override
    public void publishClaimApproved(ClaimRecord claim) {
        append(TOPIC_APPROVED, claim.getId(),
                new ClaimPayload(UUID.randomUUID().toString(), "ClaimApproved",
                        claim.getId(), claim.getTravellerId(), claim.getAssessmentId(),
                        claim.getStatus().name(), claim.getResolvedAt().toString()));
    }

    @Override
    public void publishClaimPaid(ClaimRecord claim) {
        append("claim.paid", claim.getId(),
                new ClaimPayload(UUID.randomUUID().toString(), "ClaimPaid",
                        claim.getId(), claim.getTravellerId(), claim.getAssessmentId(),
                        claim.getStatus().name(), claim.getResolvedAt().toString()));
    }

    private void append(String topic, String key, Object payload) {
        OutboxRecord r = new OutboxRecord();
        r.setId(UUID.randomUUID().toString());
        r.setTopic(topic);
        r.setAggregateId(key);
        r.setPayload(serialize(payload));
        r.setOccurredAt(Instant.now());
        outbox.save(r);
    }

    private String serialize(Object obj) {
        try { return json.writeValueAsString(obj); }
        catch (JacksonException e) { throw new IllegalStateException("Serialization failed", e); }
    }

    private record ClaimPayload(String eventId, String eventType, String claimId,
                                String travellerId, String assessmentId,
                                String status, String timestamp) {}
}
