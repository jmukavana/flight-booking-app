package com.resilire.identityconsent.infrastructure.messaging;

import com.resilire.identityconsent.domain.consent.ConsentDomainEvent;
import java.util.UUID;

/**
 * Published language for the {@code ConsentChanged} integration event consumed
 * by other contexts (e.g., Recovery's authority engine). Decoupled from the
 * internal domain event so the contract can evolve independently.
 */
public record ConsentChangedPayload(
        String eventId,
        String eventType,
        String userId,
        String scope,
        String decision,
        long version,
        String occurredAt) {

    public static final String EVENT_TYPE = "ConsentChanged";

    public static ConsentChangedPayload from(ConsentDomainEvent event) {
        return switch (event) {
            case ConsentDomainEvent.ConsentGranted g ->
                new ConsentChangedPayload(UUID.randomUUID().toString(), EVENT_TYPE,
                        g.userId().value(), g.scope().name(), "GRANTED", g.version(),
                        g.occurredAt().toString());
            case ConsentDomainEvent.ConsentRevoked r ->
                new ConsentChangedPayload(UUID.randomUUID().toString(), EVENT_TYPE,
                        r.userId().value(), r.scope().name(), "REVOKED", r.version(),
                        r.occurredAt().toString());
            case ConsentDomainEvent.ErasureRequested e ->
                new ConsentChangedPayload(UUID.randomUUID().toString(), "ErasureRequested",
                        e.userId().value(), null, "ERASURE", 0L,
                        e.occurredAt().toString());
        };
    }
}
