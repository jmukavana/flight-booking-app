package com.resilire.disruptiondetection.infrastructure.messaging;

import com.resilire.disruptiondetection.application.DisruptionDetectionService;
import com.resilire.disruptiondetection.application.FlightStatusProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class FlightStatusListener {

    private static final Logger log = LoggerFactory.getLogger(FlightStatusListener.class);

    private final DisruptionDetectionService service;
    private final ObjectMapper json;

    FlightStatusListener(DisruptionDetectionService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = "flight.status.updated")
    public void onFlightStatusUpdated(String message) {
        try {
            FlightStatusPayload payload = json.readValue(message, FlightStatusPayload.class);
            FlightStatusProjection projection = new FlightStatusProjection(
                    payload.eventId(), payload.flightId(), payload.origin(), payload.destination(),
                    payload.status(), payload.delayMinutes());
            service.onFlightStatusUpdated(projection);
        } catch (JacksonException e) {
            log.error("Failed to deserialize flight.status.updated: {}", e.getMessage());
        }
    }

    private record FlightStatusPayload(String eventId, String flightId, String origin,
                                       String destination, String status, long delayMinutes) {}
}
