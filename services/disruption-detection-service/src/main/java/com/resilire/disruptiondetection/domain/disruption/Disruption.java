package com.resilire.disruptiondetection.domain.disruption;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate representing a single disruption episode for a flight. Tracks the
 * current status (ACTIVE → RESOLVED) and the latest severity assessment.
 * Severity upgrades (MODERATE → SEVERE) are recorded via {@link #updateDelay};
 * once RESOLVED, the episode is closed — a new disruption of the same flight
 * starts a new aggregate.
 */
public class Disruption {

    /** Delay threshold for early-warning detection (Phase 1). EU261 rights kick in at destination, separately. */
    public static final int EARLY_WARNING_DELAY_MINUTES = 45;

    /** Delay threshold for SEVERE severity, aligning with EU261's destination-delay proxy. */
    public static final int SEVERE_DELAY_MINUTES = 120;

    private final DisruptionId id;
    private final FlightRef flight;
    private DisruptionStatus status;
    private DisruptionCause cause;
    private DisruptionSeverity severity;
    private int delayMinutes;
    private final Instant detectedAt;
    private Instant resolvedAt;
    private Instant lastUpdatedAt;

    private final List<DisruptionDomainEvent> domainEvents = new ArrayList<>();

    private Disruption(DisruptionId id, FlightRef flight, DisruptionStatus status,
                       DisruptionCause cause, DisruptionSeverity severity, int delayMinutes,
                       Instant detectedAt, Instant resolvedAt, Instant lastUpdatedAt) {
        this.id = id;
        this.flight = flight;
        this.status = status;
        this.cause = cause;
        this.severity = severity;
        this.delayMinutes = delayMinutes;
        this.detectedAt = detectedAt;
        this.resolvedAt = resolvedAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public static Disruption detect(DisruptionId id, FlightRef flight,
                                    DisruptionCause cause, int delayMinutes, Instant now) {
        DisruptionSeverity severity = toSeverity(cause, delayMinutes);
        Disruption disruption = new Disruption(id, flight, DisruptionStatus.ACTIVE,
                cause, severity, delayMinutes, now, null, now);
        disruption.domainEvents.add(new DisruptionDomainEvent.DisruptionDetected(
                id, flight, cause, severity, delayMinutes, now));
        return disruption;
    }

    /** Reconstitute from persistence — no events raised. */
    public static Disruption reconstitute(DisruptionId id, FlightRef flight, DisruptionStatus status,
                                          DisruptionCause cause, DisruptionSeverity severity,
                                          int delayMinutes, Instant detectedAt, Instant resolvedAt,
                                          Instant lastUpdatedAt) {
        return new Disruption(id, flight, status, cause, severity, delayMinutes,
                detectedAt, resolvedAt, lastUpdatedAt);
    }

    public void updateDelay(int newDelayMinutes, Instant now) {
        if (status != DisruptionStatus.ACTIVE) return;
        this.delayMinutes = newDelayMinutes;
        this.severity = toSeverity(cause, newDelayMinutes);
        this.lastUpdatedAt = now;
    }

    public void resolve(Instant now) {
        if (status == DisruptionStatus.RESOLVED) return;
        this.status = DisruptionStatus.RESOLVED;
        this.resolvedAt = now;
        this.lastUpdatedAt = now;
        domainEvents.add(new DisruptionDomainEvent.DisruptionResolved(id, flight, now));
    }

    public List<DisruptionDomainEvent> pullDomainEvents() {
        List<DisruptionDomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    private static DisruptionSeverity toSeverity(DisruptionCause cause, int delayMinutes) {
        if (cause == DisruptionCause.FLIGHT_CANCELLED) return DisruptionSeverity.CANCELLED;
        if (delayMinutes >= SEVERE_DELAY_MINUTES) return DisruptionSeverity.SEVERE;
        return DisruptionSeverity.MODERATE;
    }

    public DisruptionId id() { return id; }
    public FlightRef flight() { return flight; }
    public DisruptionStatus status() { return status; }
    public DisruptionCause cause() { return cause; }
    public DisruptionSeverity severity() { return severity; }
    public int delayMinutes() { return delayMinutes; }
    public Instant detectedAt() { return detectedAt; }
    public Instant resolvedAt() { return resolvedAt; }
    public Instant lastUpdatedAt() { return lastUpdatedAt; }
}
