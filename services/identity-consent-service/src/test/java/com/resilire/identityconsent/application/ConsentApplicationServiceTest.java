package com.resilire.identityconsent.application;

import com.resilire.identityconsent.domain.consent.ConsentDomainEvent;
import com.resilire.identityconsent.domain.consent.ConsentEntry;
import com.resilire.identityconsent.domain.consent.ConsentLedger;
import com.resilire.identityconsent.domain.consent.ConsentLedgerRepository;
import com.resilire.identityconsent.domain.consent.ConsentNotGrantedException;
import com.resilire.identityconsent.domain.consent.ConsentScope;
import com.resilire.identityconsent.domain.consent.UserId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsentApplicationServiceTest {

    private StubConsentLedgerRepository ledgers;
    private StubEventPublisher publisher;
    private ConsentApplicationService service;

    private final UserId user = UserId.of("traveller-1");

    @BeforeEach
    void setUp() {
        ledgers = new StubConsentLedgerRepository();
        publisher = new StubEventPublisher();
        service = new ConsentApplicationService(ledgers, publisher);
    }

    @Test
    void grant_consent_makes_scope_effective_and_publishes_event() {
        service.grantConsent(user, ConsentScope.AUTO_RECOVERY);

        Set<ConsentScope> effective = service.effectiveConsents(user);
        assertThat(effective).contains(ConsentScope.AUTO_RECOVERY);
        assertThat(publisher.events).singleElement().isInstanceOf(ConsentDomainEvent.ConsentGranted.class);
    }

    @Test
    void grant_is_idempotent_for_same_scope() {
        service.grantConsent(user, ConsentScope.MARKETING);
        int eventsBefore = publisher.events.size();

        service.grantConsent(user, ConsentScope.MARKETING);

        assertThat(publisher.events).hasSize(eventsBefore);
    }

    @Test
    void revoke_removes_effective_scope_and_publishes_event() {
        service.grantConsent(user, ConsentScope.DATA_PROCESSING);
        publisher.events.clear();

        service.revokeConsent(user, ConsentScope.DATA_PROCESSING);

        assertThat(service.effectiveConsents(user)).doesNotContain(ConsentScope.DATA_PROCESSING);
        assertThat(publisher.events).singleElement().isInstanceOf(ConsentDomainEvent.ConsentRevoked.class);
    }

    @Test
    void revoking_never_granted_scope_throws() {
        assertThatThrownBy(() -> service.revokeConsent(user, ConsentScope.THIRD_PARTY_CLAIMS))
                .isInstanceOf(ConsentNotGrantedException.class);
    }

    @Test
    void history_reflects_all_operations_in_order() {
        service.grantConsent(user, ConsentScope.AUTO_RECOVERY);
        service.grantConsent(user, ConsentScope.MARKETING);
        service.revokeConsent(user, ConsentScope.MARKETING);

        List<ConsentEntry> history = service.history(user);
        assertThat(history).hasSize(3);
        assertThat(history.get(0).scope()).isEqualTo(ConsentScope.AUTO_RECOVERY);
        assertThat(history.get(1).scope()).isEqualTo(ConsentScope.MARKETING);
        assertThat(history.get(2).scope()).isEqualTo(ConsentScope.MARKETING);
        assertThat(history.get(2).grants()).isFalse();
    }

    // --- stubs ---

    static class StubConsentLedgerRepository implements ConsentLedgerRepository {
        private final Map<String, ConsentLedger> ledgers = new HashMap<>();

        @Override
        public ConsentLedger load(UserId userId) {
            return ledgers.computeIfAbsent(userId.value(), k -> ConsentLedger.empty(userId));
        }

        @Override
        public void save(ConsentLedger ledger) {
            ledgers.put(ledger.userId().value(), ledger);
        }

        @Override
        public List<ConsentEntry> history(UserId userId) {
            ConsentLedger ledger = ledgers.get(userId.value());
            if (ledger == null) return List.of();
            return ledger.uncommittedEntries();
        }
    }

    static class StubEventPublisher implements IntegrationEventPublisher {
        final List<ConsentDomainEvent> events = new ArrayList<>();

        @Override public void publish(ConsentDomainEvent event) { events.add(event); }
    }
}
