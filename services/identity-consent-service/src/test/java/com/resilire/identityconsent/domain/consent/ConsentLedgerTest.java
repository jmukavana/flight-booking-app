package com.resilire.identityconsent.domain.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Aggregate invariants — pure domain, no Spring, fast. */
class ConsentLedgerTest {

    private final UserId user = UserId.of("traveller-1");

    @Test
    void grant_appends_entry_and_raises_event() {
        ConsentLedger ledger = ConsentLedger.empty(user);

        ledger.grant(ConsentScope.AUTO_RECOVERY);

        assertThat(ledger.isEffective(ConsentScope.AUTO_RECOVERY)).isTrue();
        assertThat(ledger.uncommittedEntries()).hasSize(1);
        assertThat(ledger.pullDomainEvents())
                .singleElement()
                .isInstanceOf(ConsentDomainEvent.ConsentGranted.class);
    }

    @Test
    void granting_an_already_effective_scope_is_a_no_op() {
        ConsentLedger ledger = ConsentLedger.empty(user);
        ledger.grant(ConsentScope.MARKETING);
        ledger.pullDomainEvents();

        ledger.grant(ConsentScope.MARKETING);

        assertThat(ledger.uncommittedEntries()).hasSize(1);
        assertThat(ledger.pullDomainEvents()).isEmpty();
    }

    @Test
    void revoke_requires_an_effective_consent() {
        ConsentLedger ledger = ConsentLedger.empty(user);

        assertThatThrownBy(() -> ledger.revoke(ConsentScope.DATA_PROCESSING))
                .isInstanceOf(ConsentNotGrantedException.class);
    }

    @Test
    void revoke_after_grant_clears_effective_and_raises_event() {
        ConsentLedger ledger = ConsentLedger.empty(user);
        ledger.grant(ConsentScope.THIRD_PARTY_CLAIMS);

        ledger.revoke(ConsentScope.THIRD_PARTY_CLAIMS);

        assertThat(ledger.isEffective(ConsentScope.THIRD_PARTY_CLAIMS)).isFalse();
        assertThat(ledger.pullDomainEvents())
                .last()
                .isInstanceOf(ConsentDomainEvent.ConsentRevoked.class);
    }

    @Test
    void rehydrate_folds_history_and_keeps_version_monotonic() {
        ConsentLedger seed = ConsentLedger.empty(user);
        seed.grant(ConsentScope.MARKETING);
        seed.revoke(ConsentScope.MARKETING);
        seed.grant(ConsentScope.AUTO_RECOVERY);
        List<ConsentEntry> history = seed.uncommittedEntries();

        ConsentLedger rebuilt = ConsentLedger.rehydrate(user, history);
        rebuilt.grant(ConsentScope.DATA_PROCESSING);

        assertThat(rebuilt.isEffective(ConsentScope.AUTO_RECOVERY)).isTrue();
        assertThat(rebuilt.isEffective(ConsentScope.MARKETING)).isFalse();
        assertThat(rebuilt.uncommittedEntries())
                .singleElement()
                .extracting(ConsentEntry::version)
                .isEqualTo(4L);
    }
}
