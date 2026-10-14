package com.resilire.identityconsent.domain.consent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregate root for one Traveller's consent decisions. The ledger is
 * append-only: {@link #grant}/{@link #revoke} never mutate or remove a
 * {@link ConsentEntry}, they append a new one. Effective consent is a
 * projection (fold) over the history — the latest entry per scope wins.
 *
 * <p>Reconstituted from history via {@link #rehydrate}; new facts produced in a
 * unit of work are exposed through {@link #uncommittedEntries()} and
 * {@link #pullDomainEvents()} for the application layer to persist and publish.
 */
public final class ConsentLedger {

    private final UserId userId;
    private final Map<ConsentScope, ConsentEntry> effectiveByScope = new EnumMap<>(ConsentScope.class);
    private long lastVersion;

    private final List<ConsentEntry> uncommitted = new ArrayList<>();
    private final List<ConsentDomainEvent> events = new ArrayList<>();

    private ConsentLedger(UserId userId) {
        this.userId = userId;
    }

    /** A ledger with no prior history. */
    public static ConsentLedger empty(UserId userId) {
        return new ConsentLedger(userId);
    }

    /** Rebuild from the persisted, ordered append-only history. */
    public static ConsentLedger rehydrate(UserId userId, List<ConsentEntry> history) {
        ConsentLedger ledger = new ConsentLedger(userId);
        for (ConsentEntry entry : history) {
            ledger.apply(entry);
        }
        return ledger;
    }

    private void apply(ConsentEntry entry) {
        if (entry.grants()) {
            effectiveByScope.put(entry.scope(), entry);
        } else {
            effectiveByScope.remove(entry.scope());
        }
        lastVersion = Math.max(lastVersion, entry.version());
    }

    /**
     * Record consent for {@code scope}. Idempotent: granting an already-effective
     * scope is a no-op (no entry, no event), so the ledger stays meaningful.
     */
    public void grant(ConsentScope scope) {
        if (isEffective(scope)) {
            return;
        }
        ConsentEntry entry = append(scope, ConsentDecision.GRANT);
        events.add(new ConsentDomainEvent.ConsentGranted(
                userId, scope, entry.version(), entry.occurredAt()));
    }

    /**
     * Withdraw consent for {@code scope}.
     *
     * @throws ConsentNotGrantedException if the scope is not currently effective
     */
    public void revoke(ConsentScope scope) {
        if (!isEffective(scope)) {
            throw new ConsentNotGrantedException(userId, scope);
        }
        ConsentEntry entry = append(scope, ConsentDecision.REVOKE);
        events.add(new ConsentDomainEvent.ConsentRevoked(
                userId, scope, entry.version(), entry.occurredAt()));
    }

    private ConsentEntry append(ConsentScope scope, ConsentDecision decision) {
        ConsentEntry entry = ConsentEntry.record(scope, decision, ++lastVersion);
        apply(entry);
        uncommitted.add(entry);
        return entry;
    }

    public boolean isEffective(ConsentScope scope) {
        return effectiveByScope.containsKey(scope);
    }

    public Set<ConsentScope> effectiveScopes() {
        return Set.copyOf(effectiveByScope.keySet());
    }

    public UserId userId() {
        return userId;
    }

    /** New entries produced in this unit of work, to be appended to the store. */
    public List<ConsentEntry> uncommittedEntries() {
        return List.copyOf(uncommitted);
    }

    /**
     * Raise a GDPR Art. 17 erasure request. Does not alter consent entries — the
     * downstream erasure handler anonymises all personal data upon receiving the event.
     * Can be called regardless of current consent state (even with no active scopes).
     */
    public void requestErasure() {
        events.add(new ConsentDomainEvent.ErasureRequested(userId, Instant.now()));
    }

    /** Drains domain events so the application layer can publish them once. */
    public List<ConsentDomainEvent> pullDomainEvents() {
        List<ConsentDomainEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }
}
