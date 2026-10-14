package com.resilire.identityconsent.domain.consent;

import java.time.Instant;

/**
 * Domain events raised by the {@link ConsentLedger} aggregate when effective
 * consent changes. The application layer translates these into the
 * {@code ConsentChanged} integration event (published language) via the outbox.
 */
public sealed interface ConsentDomainEvent
        permits ConsentDomainEvent.ConsentGranted, ConsentDomainEvent.ConsentRevoked,
                ConsentDomainEvent.ErasureRequested {

    UserId userId();

    Instant occurredAt();

    record ConsentGranted(UserId userId, ConsentScope scope, long version, Instant occurredAt)
            implements ConsentDomainEvent {}

    record ConsentRevoked(UserId userId, ConsentScope scope, long version, Instant occurredAt)
            implements ConsentDomainEvent {}

    /** GDPR Art. 17 right-to-erasure signal. Downstream services receiving this via Kafka
     *  must anonymise or delete all personal data associated with {@link #userId()}. */
    record ErasureRequested(UserId userId, Instant occurredAt) implements ConsentDomainEvent {}
}
