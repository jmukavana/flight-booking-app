package com.resilire.ingestion.domain.feed;

import java.time.Instant;
import java.util.Set;

public record WeatherAlertEvent(
        String alertId,
        AlertSeverity severity,
        Set<AirportCode> affectedAirports,
        String description,
        Instant validFrom,
        Instant validUntil,
        String source,
        Instant normalisedAt) {
}
