package com.resilire.payments.infrastructure.persistence;

import com.resilire.payments.domain.payment.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only persistence row for a {@link com.resilire.payments.domain.payment.LedgerEntry}.
 * No setters and no update path — the adapter only ever inserts (SDD §4: the ledger
 * is immutable). The {@code (idempotency_key, type)} unique constraint turns a racing
 * duplicate capture/refund into a constraint violation rather than a double movement;
 * capture and refund can share the booking's key because they differ by type.
 */
@Entity
@Table(name = "payment_ledger",
        uniqueConstraints = @UniqueConstraint(name = "uq_ledger_key_type",
                columnNames = {"idempotency_key", "type"}))
public class LedgerEntryRecord {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Column(name = "traveller_id", nullable = false)
    private String travellerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "processor_ref", nullable = false)
    private String processorRef;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "related_ref")
    private String relatedRef;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected LedgerEntryRecord() {
        // for JPA
    }

    public LedgerEntryRecord(UUID id, PaymentType type, String travellerId, BigDecimal amount,
                             String currency, String processorRef, String idempotencyKey,
                             String relatedRef, Instant recordedAt) {
        this.id = id;
        this.type = type;
        this.travellerId = travellerId;
        this.amount = amount;
        this.currency = currency;
        this.processorRef = processorRef;
        this.idempotencyKey = idempotencyKey;
        this.relatedRef = relatedRef;
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public PaymentType getType() {
        return type;
    }

    public String getTravellerId() {
        return travellerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProcessorRef() {
        return processorRef;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRelatedRef() {
        return relatedRef;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
