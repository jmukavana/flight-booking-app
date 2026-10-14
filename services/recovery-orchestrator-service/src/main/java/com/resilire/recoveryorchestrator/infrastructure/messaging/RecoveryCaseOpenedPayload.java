package com.resilire.recoveryorchestrator.infrastructure.messaging;

import java.time.Instant;
import java.util.List;

record RecoveryCaseOpenedPayload(
        String caseId,
        String disruptionId,
        String flightId,
        String severity,
        List<String> proposedOptions,
        Instant openedAt) {

    static final String TOPIC = "recovery.case.opened";
}
