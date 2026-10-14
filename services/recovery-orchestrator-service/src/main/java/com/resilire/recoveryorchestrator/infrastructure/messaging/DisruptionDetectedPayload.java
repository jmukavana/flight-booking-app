package com.resilire.recoveryorchestrator.infrastructure.messaging;

record DisruptionDetectedPayload(
        String disruptionId,
        String flightId,
        String origin,
        String destination,
        String cause,
        String severity,
        int delayMinutes) {}
