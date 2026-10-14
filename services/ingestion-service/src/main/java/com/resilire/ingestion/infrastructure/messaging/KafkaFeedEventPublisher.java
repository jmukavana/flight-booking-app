package com.resilire.ingestion.infrastructure.messaging;

import com.resilire.ingestion.application.FeedEventPublisher;
import com.resilire.ingestion.domain.feed.FlightStatusEvent;
import com.resilire.ingestion.domain.feed.WeatherAlertEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class KafkaFeedEventPublisher implements FeedEventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    KafkaFeedEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    @Override
    public void publishFlightStatus(FlightStatusEvent event) {
        FlightStatusPayload payload = FlightStatusPayload.from(event);
        kafka.send(FlightStatusPayload.TOPIC, payload.flightId(), serialize(payload));
    }

    @Override
    public void publishWeatherAlert(WeatherAlertEvent event) {
        WeatherAlertPayload payload = WeatherAlertPayload.from(event);
        kafka.send(WeatherAlertPayload.TOPIC, payload.alertId(), serialize(payload));
    }

    private String serialize(Object payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize feed event", e);
        }
    }
}
