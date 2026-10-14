package com.resilire.ingestion.application;

import com.resilire.ingestion.domain.feed.AirportCode;
import com.resilire.ingestion.domain.feed.AlertSeverity;
import com.resilire.ingestion.domain.feed.FlightId;
import com.resilire.ingestion.domain.feed.FlightStatusCode;
import com.resilire.ingestion.domain.feed.FlightStatusEvent;
import com.resilire.ingestion.domain.feed.WeatherAlertEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Normalises raw external feed payloads into typed domain events and publishes
 * them to the feed event bus. Carrier-specific status strings map to the
 * canonical {@link FlightStatusCode}; unrecognised values fall back to UNKNOWN
 * rather than failing the pipeline.
 */
@Service
public class FeedNormalisationService {

    private final FeedEventPublisher publisher;
    private final Clock clock;

    public FeedNormalisationService(FeedEventPublisher publisher) {
        this(publisher, Clock.systemUTC());
    }

    FeedNormalisationService(FeedEventPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    public void normaliseFlightStatus(NormaliseFlightStatusCommand cmd) {
        Instant scheduled = Instant.parse(cmd.scheduledDeparture());
        Instant estimated = cmd.estimatedDeparture() != null
                ? Instant.parse(cmd.estimatedDeparture())
                : scheduled;
        long delayMinutes = Math.max(0, Duration.between(scheduled, estimated).toMinutes());

        FlightStatusEvent event = new FlightStatusEvent(
                FlightId.of(cmd.flightNumber()),
                AirportCode.of(cmd.origin()),
                AirportCode.of(cmd.destination()),
                parseStatus(cmd.status()),
                scheduled,
                estimated,
                Duration.ofMinutes(delayMinutes),
                cmd.source(),
                Instant.now(clock));
        publisher.publishFlightStatus(event);
    }

    public void normaliseWeatherAlert(NormaliseWeatherAlertCommand cmd) {
        Set<AirportCode> airports = cmd.airports().stream()
                .map(AirportCode::of)
                .collect(Collectors.toUnmodifiableSet());

        WeatherAlertEvent event = new WeatherAlertEvent(
                cmd.alertId(),
                parseSeverity(cmd.severity()),
                airports,
                cmd.description(),
                Instant.parse(cmd.validFrom()),
                Instant.parse(cmd.validUntil()),
                cmd.source(),
                Instant.now(clock));
        publisher.publishWeatherAlert(event);
    }

    private FlightStatusCode parseStatus(String raw) {
        if (raw == null) return FlightStatusCode.UNKNOWN;
        try {
            return FlightStatusCode.valueOf(raw.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return FlightStatusCode.UNKNOWN;
        }
    }

    private AlertSeverity parseSeverity(String raw) {
        if (raw == null) return AlertSeverity.LOW;
        try {
            return AlertSeverity.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AlertSeverity.LOW;
        }
    }
}
