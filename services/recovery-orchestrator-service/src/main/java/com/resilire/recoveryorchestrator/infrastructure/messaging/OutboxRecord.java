package com.resilire.recoveryorchestrator.infrastructure.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recovery_outbox")
@Getter
@Setter
@NoArgsConstructor
public class OutboxRecord {
    @Id private String id;
    @Column(nullable = false) private String topic;
    @Column(name = "aggregate_id", nullable = false) private String aggregateId;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "published_at") private Instant publishedAt;

    public void markPublished(Instant at) { this.publishedAt = at; }
}
