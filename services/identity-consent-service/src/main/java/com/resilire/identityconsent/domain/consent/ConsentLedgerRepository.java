package com.resilire.identityconsent.domain.consent;

import java.util.List;

/**
 * Outbound port owned by the domain. Implemented by an infrastructure adapter.
 * {@code save} persists only the aggregate's uncommitted (new) entries — the
 * store is append-only, existing entries are never rewritten.
 */
public interface ConsentLedgerRepository {

    ConsentLedger load(UserId userId);

    void save(ConsentLedger ledger);

    /** The full immutable, ordered history for GDPR transparency. */
    List<ConsentEntry> history(UserId userId);
}
