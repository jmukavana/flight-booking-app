package com.resilire.ingestion.infrastructure.messaging;

import com.resilire.ingestion.domain.feed.WeatherAlertEvent;
import java.util.List;

/**
 * Published language for the {@code weather.alert.published} integration event.
 * Consumed by {@code disruption-detection-service} to factor weather into
 * the disruption state machine.
 */
public record WeatherAlertPayload(
        String eventId,
        String eventType,
        String alertId,
        String severity,
        List<String> airports,
        String description,
        String validFrom,
        String validUntil,
        String source,
        String normalisedAt) {

    public static final String EVENT_TYPE = "WeatherAlertPublished";
    public static final String TOPIC = "weather.alert.published";

    public static WeatherAlertPayload from(WeatherAlertEvent e) {
        List<String> airports = e.affectedAirports().stream()
                .map(a -> a.value())
                .sorted()
                .toList();
        return new WeatherAlertPayload(
                java.util.UUID.randomUUID().toString(),
                EVENT_TYPE,
                e.alertId(),
                e.severity().name(),
                airports,
                e.description(),
                e.validFrom().toString(),
                e.validUntil().toString(),
                e.source(),
                e.normalisedAt().toString());
    }
}
