package com.resilire.identityconsent.domain.consent;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable fact in the consent ledger: "at {@code occurredAt}, the Traveller
 * {@code decision}-ed {@code scope}". Never updated or deleted. {@code version}
 * is the Traveller-local sequence number that orders the ledger and guards
 * against concurrent appends.
 */
public record ConsentEntry(
        UUID id,
        ConsentScope scope,
        ConsentDecision decision,
        long version,
        Instant occurredAt) {

    public static ConsentEntry record(ConsentScope scope, ConsentDecision decision, long version) {
        return new ConsentEntry(UUID.randomUUID(), scope, decision, version, Instant.now());
    }

    public boolean grants() {
        return decision == ConsentDecision.GRANT;
    }
}
