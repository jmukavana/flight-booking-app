package com.resilire.recoveryorchestrator.infrastructure.messaging;

import com.resilire.recoveryorchestrator.application.RecoveryOrchestratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class DisruptionEventListener {

    private static final Logger log = LoggerFactory.getLogger(DisruptionEventListener.class);

    private final RecoveryOrchestratorService service;
    private final ObjectMapper mapper;

    DisruptionEventListener(RecoveryOrchestratorService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "disruption.detected", groupId = "${spring.kafka.consumer.group-id}")
    public void onDisruptionDetected(String message) {
        try {
            DisruptionDetectedPayload payload = mapper.readValue(message, DisruptionDetectedPayload.class);
            service.openCase(payload.disruptionId(), payload.flightId(),
                    payload.origin(), payload.destination(),
                    payload.cause(), payload.severity(), payload.delayMinutes());
        } catch (Exception e) {
            log.error("Failed to process disruption.detected: {}", e.getMessage());
        }
    }
}
