package com.resilire.disruptiondetection.domain.disruption;

import java.time.Instant;

public sealed interface DisruptionDomainEvent
        permits DisruptionDomainEvent.DisruptionDetected, DisruptionDomainEvent.DisruptionResolved {

    DisruptionId disruptionId();
    FlightRef flight();

    record DisruptionDetected(
            DisruptionId disruptionId,
            FlightRef flight,
            DisruptionCause cause,
            DisruptionSeverity severity,
            int delayMinutes,
            Instant detectedAt) implements DisruptionDomainEvent {}

    record DisruptionResolved(
            DisruptionId disruptionId,
            FlightRef flight,
            Instant resolvedAt) implements DisruptionDomainEvent {}
}
