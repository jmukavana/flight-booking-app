package com.resilire.payments.domain.payment;

import java.util.Objects;
import java.util.UUID;

/** Identity of a ledger entry. Value object. */
public record LedgerEntryId(UUID value) {

    public LedgerEntryId {
        Objects.requireNonNull(value, "ledgerEntryId value");
    }

    public static LedgerEntryId newId() {
        return new LedgerEntryId(UUID.randomUUID());
    }

    public static LedgerEntryId of(UUID value) {
        return new LedgerEntryId(value);
    }
}
