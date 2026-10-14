package com.resilire.disruptiondetection.infrastructure.messaging;

import com.resilire.disruptiondetection.domain.disruption.DisruptionDomainEvent;
import java.util.UUID;

/**
 * Published language for the {@code disruption.detected} integration event.
 * Consumed by notification-service (alerts travellers) and recovery-orchestrator
 * (initiates assisted rebooking). Keyed by flightId for per-flight ordering.
 */
public record DisruptionDetectedPayload(
        String eventId,
        String eventType,
        String disruptionId,
        String flightId,
        String origin,
        String destination,
        String cause,
        String severity,
        int delayMinutes,
        String detectedAt) {

    public static final String EVENT_TYPE = "DisruptionDetected";
    public static final String TOPIC = "disruption.detected";

    public static DisruptionDetectedPayload from(DisruptionDomainEvent.DisruptionDetected e) {
        return new DisruptionDetectedPayload(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                e.disruptionId().value(),
                e.flight().flightId(),
                e.flight().origin(),
                e.flight().destination(),
                e.cause().name(),
                e.severity().name(),
                e.delayMinutes(),
                e.detectedAt().toString());
    }
}
