package com.resilire.identityconsent.infrastructure.persistence;

import com.resilire.identityconsent.domain.consent.ConsentEntry;
import com.resilire.identityconsent.domain.consent.ConsentLedger;
import com.resilire.identityconsent.domain.consent.ConsentLedgerRepository;
import com.resilire.identityconsent.domain.consent.UserId;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter implementing the domain port. Translates between the
 * append-only {@link ConsentEntryRecord} rows and the {@link ConsentLedger}
 * aggregate, and persists only the aggregate's new (uncommitted) entries.
 */
@Repository
public class JpaConsentLedgerRepository implements ConsentLedgerRepository {

    private final ConsentEntryJpaRepository rows;

    public JpaConsentLedgerRepository(ConsentEntryJpaRepository rows) {
        this.rows = rows;
    }

    @Override
    public ConsentLedger load(UserId userId) {
        return ConsentLedger.rehydrate(userId, history(userId));
    }

    @Override
    public void save(ConsentLedger ledger) {
        String userId = ledger.userId().value();
        ledger.uncommittedEntries().stream()
                .map(entry -> toRow(userId, entry))
                .forEach(rows::save);
    }

    @Override
    public List<ConsentEntry> history(UserId userId) {
        return rows.findByUserIdOrderByVersionAsc(userId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private ConsentEntryRecord toRow(String userId, ConsentEntry entry) {
        return new ConsentEntryRecord(entry.id(), userId, entry.scope(),
                entry.decision(), entry.version(), entry.occurredAt());
    }

    private ConsentEntry toDomain(ConsentEntryRecord row) {
        return new ConsentEntry(row.getId(), row.getScope(), row.getDecision(),
                row.getVersion(), row.getOccurredAt());
    }
}
