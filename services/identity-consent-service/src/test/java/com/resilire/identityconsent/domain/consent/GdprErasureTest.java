package com.resilire.identityconsent.domain.consent;

import com.resilire.identityconsent.application.ConsentApplicationService;
import com.resilire.identityconsent.application.IntegrationEventPublisher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GdprErasureTest {

    private StubLedgerRepository ledgers;
    private StubPublisher publisher;
    private ConsentApplicationService service;

    private final UserId user = UserId.of("traveller-gdpr-1");

    @BeforeEach
    void setUp() {
        ledgers = new StubLedgerRepository();
        publisher = new StubPublisher();
        service = new ConsentApplicationService(ledgers, publisher);
    }

    @Test
    void erasure_request_publishes_ErasureRequested_event() {
        service.requestErasure(user);

        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.get(0))
                .isInstanceOf(ConsentDomainEvent.ErasureRequested.class);
        ConsentDomainEvent.ErasureRequested event =
                (ConsentDomainEvent.ErasureRequested) publisher.events.get(0);
        assertThat(event.userId()).isEqualTo(user);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void erasure_request_works_with_no_active_consents() {
        service.requestErasure(user);

        assertThat(publisher.events).singleElement()
                .isInstanceOf(ConsentDomainEvent.ErasureRequested.class);
    }

    @Test
    void erasure_request_works_after_consents_were_granted() {
        service.grantConsent(user, ConsentScope.AUTO_RECOVERY);
        service.grantConsent(user, ConsentScope.MARKETING);
        publisher.events.clear();

        service.requestErasure(user);

        assertThat(publisher.events).singleElement()
                .isInstanceOf(ConsentDomainEvent.ErasureRequested.class);
        assertThat(service.effectiveConsents(user)).isNotEmpty();
    }

    @Test
    void each_erasure_call_produces_one_event() {
        service.requestErasure(user);
        int after_first = publisher.events.size();

        publisher.events.clear();
        service.requestErasure(user);

        assertThat(after_first).isEqualTo(1);
        assertThat(publisher.events).hasSize(1);
    }

    static class StubLedgerRepository implements ConsentLedgerRepository {
        private final Map<String, ConsentLedger> store = new HashMap<>();

        @Override public ConsentLedger load(UserId id) {
            return store.computeIfAbsent(id.value(), k -> ConsentLedger.empty(id));
        }
        @Override public void save(ConsentLedger ledger) { store.put(ledger.userId().value(), ledger); }
        @Override public List<ConsentEntry> history(UserId id) {
            ConsentLedger l = store.get(id.value());
            return l == null ? List.of() : l.uncommittedEntries();
        }
    }

    static class StubPublisher implements IntegrationEventPublisher {
        final List<ConsentDomainEvent> events = new ArrayList<>();

        @Override public void publish(ConsentDomainEvent e) { events.add(e); }
    }
}
