package com.resilire.identityconsent.infrastructure.persistence;

import com.resilire.identityconsent.domain.consent.ConsentDecision;
import com.resilire.identityconsent.domain.consent.ConsentScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only persistence row for one {@link com.resilire.identityconsent.domain.consent.ConsentEntry}.
 * No setters for business fields and no update path — the adapter only ever
 * inserts. The {@code (user_id, version)} unique constraint turns a concurrent
 * double-append into a constraint violation rather than a lost write.
 */
@Entity
@Table(name = "consent_entry",
        uniqueConstraints = @UniqueConstraint(name = "uq_consent_user_version",
                columnNames = {"user_id", "version"}))
public class ConsentEntryRecord {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentDecision decision;

    @Column(nullable = false)
    private long version;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ConsentEntryRecord() {
        // for JPA
    }

    public ConsentEntryRecord(UUID id, String userId, ConsentScope scope,
                              ConsentDecision decision, long version, Instant occurredAt) {
        this.id = id;
        this.userId = userId;
        this.scope = scope;
        this.decision = decision;
        this.version = version;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public ConsentScope getScope() {
        return scope;
    }

    public ConsentDecision getDecision() {
        return decision;
    }

    public long getVersion() {
        return version;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
