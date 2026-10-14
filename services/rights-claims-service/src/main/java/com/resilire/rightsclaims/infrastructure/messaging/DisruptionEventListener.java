package com.resilire.rightsclaims.infrastructure.messaging;

import com.resilire.rightsclaims.application.AssessmentRequest;
import com.resilire.rightsclaims.application.EligibilityService;
import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.Jurisdiction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Auto-assesses eligibility when a disruption is detected. The assessment is
 * persisted immediately so it is available when the recovery-orchestrator asks
 * for options. Duplicate delivery is safe — {@link EligibilityService#assess}
 * is idempotent on disruptionId.
 *
 * <p>Default jurisdiction: EU261 (Phase 2 launches EU-first). Phase 3 adds
 * a JurisdictionResolver based on origin/destination airport-country mapping.
 */
@Component
class DisruptionEventListener {

    private static final Logger log = LoggerFactory.getLogger(DisruptionEventListener.class);

    private final EligibilityService eligibilityService;
    private final ObjectMapper json;

    DisruptionEventListener(EligibilityService eligibilityService, ObjectMapper json) {
        this.eligibilityService = eligibilityService;
        this.json = json;
    }

    @KafkaListener(topics = "disruption.detected")
    public void onDisruptionDetected(String message) {
        try {
            DisruptionPayload payload = json.readValue(message, DisruptionPayload.class);

            AssessmentRequest request = new AssessmentRequest(
                    payload.disruptionId(),
                    payload.flightId(),
                    payload.origin(),
                    payload.destination(),
                    Jurisdiction.EU261,
                    payload.delayMinutes(),
                    DisruptionCause.CONTROLLABLE,
                    null);

            eligibilityService.assess(request);
        } catch (JacksonException e) {
            log.error("Failed to deserialize disruption.detected: {}", e.getMessage());
        }
    }

    private record DisruptionPayload(String disruptionId, String flightId, String origin,
                                     String destination, String cause, int delayMinutes) {}
}
