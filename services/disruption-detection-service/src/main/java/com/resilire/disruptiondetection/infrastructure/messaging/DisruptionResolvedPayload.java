package com.resilire.disruptiondetection.infrastructure.messaging;

import com.resilire.disruptiondetection.domain.disruption.DisruptionDomainEvent;
import java.util.UUID;

/** Published language for the {@code disruption.resolved} integration event. */
public record DisruptionResolvedPayload(
        String eventId,
        String eventType,
        String disruptionId,
        String flightId,
        String origin,
        String destination,
        String resolvedAt) {

    public static final String EVENT_TYPE = "DisruptionResolved";
    public static final String TOPIC = "disruption.resolved";

    public static DisruptionResolvedPayload from(DisruptionDomainEvent.DisruptionResolved e) {
        return new DisruptionResolvedPayload(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                e.disruptionId().value(),
                e.flight().flightId(),
                e.flight().origin(),
                e.flight().destination(),
                e.resolvedAt().toString());
    }
}
