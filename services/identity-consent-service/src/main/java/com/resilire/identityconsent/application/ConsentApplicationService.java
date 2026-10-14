package com.resilire.identityconsent.application;

import com.resilire.identityconsent.domain.consent.ConsentDomainEvent;
import com.resilire.identityconsent.domain.consent.ConsentEntry;
import com.resilire.identityconsent.domain.consent.ConsentLedger;
import com.resilire.identityconsent.domain.consent.ConsentLedgerRepository;
import com.resilire.identityconsent.domain.consent.ConsentScope;
import com.resilire.identityconsent.domain.consent.UserId;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the Identity &amp; Consent context. Each command is one
 * unit of work: load the aggregate, let it enforce its invariants, persist the
 * new append-only entries, and publish the resulting domain events through the
 * outbox — all in one transaction.
 */
@Service
public class ConsentApplicationService {

    private final ConsentLedgerRepository ledgers;
    private final IntegrationEventPublisher events;

    public ConsentApplicationService(ConsentLedgerRepository ledgers,
                                      IntegrationEventPublisher events) {
        this.ledgers = ledgers;
        this.events = events;
    }

    @Transactional
    public void grantConsent(UserId userId, ConsentScope scope) {
        ConsentLedger ledger = ledgers.load(userId);
        ledger.grant(scope);
        commit(ledger);
    }

    @Transactional
    public void revokeConsent(UserId userId, ConsentScope scope) {
        ConsentLedger ledger = ledgers.load(userId);
        ledger.revoke(scope);
        commit(ledger);
    }

    @Transactional(readOnly = true)
    public Set<ConsentScope> effectiveConsents(UserId userId) {
        return ledgers.load(userId).effectiveScopes();
    }

    @Transactional
    public void requestErasure(UserId userId) {
        ConsentLedger ledger = ledgers.load(userId);
        ledger.requestErasure();
        commit(ledger);
    }

    @Transactional(readOnly = true)
    public List<ConsentEntry> history(UserId userId) {
        return ledgers.history(userId);
    }

    private void commit(ConsentLedger ledger) {
        ledgers.save(ledger);
        for (ConsentDomainEvent event : ledger.pullDomainEvents()) {
            events.publish(event);
        }
    }
}
