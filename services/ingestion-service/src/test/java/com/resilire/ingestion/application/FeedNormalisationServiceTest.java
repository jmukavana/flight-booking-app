package com.resilire.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.resilire.ingestion.domain.feed.AlertSeverity;
import com.resilire.ingestion.domain.feed.FlightStatusCode;
import com.resilire.ingestion.domain.feed.FlightStatusEvent;
import com.resilire.ingestion.domain.feed.WeatherAlertEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeedNormalisationServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-23T10:00:00Z");

    private FakePublisher publisher;
    private FeedNormalisationService service;

    @BeforeEach
    void setUp() {
        publisher = new FakePublisher();
        service = new FeedNormalisationService(publisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void flight_status_normalised_and_published() {
        service.normaliseFlightStatus(new NormaliseFlightStatusCommand(
                "KQ101", "NBO", "LHR", "delayed",
                "2026-05-23T08:00:00Z", "2026-05-23T09:30:00Z", "oag"));

        assertThat(publisher.flightEvents).hasSize(1);
        FlightStatusEvent e = publisher.flightEvents.get(0);
        assertThat(e.flightId().value()).isEqualTo("KQ101");
        assertThat(e.origin().value()).isEqualTo("NBO");
        assertThat(e.destination().value()).isEqualTo("LHR");
        assertThat(e.status()).isEqualTo(FlightStatusCode.DELAYED);
        assertThat(e.delay().toMinutes()).isEqualTo(90);
        assertThat(e.normalisedAt()).isEqualTo(NOW);
    }

    @Test
    void on_time_flight_has_zero_delay() {
        service.normaliseFlightStatus(new NormaliseFlightStatusCommand(
                "KQ201", "LHR", "NBO", "on_time",
                "2026-05-23T14:00:00Z", "2026-05-23T14:00:00Z", "oag"));

        FlightStatusEvent e = publisher.flightEvents.get(0);
        assertThat(e.status()).isEqualTo(FlightStatusCode.ON_TIME);
        assertThat(e.delay().toMinutes()).isZero();
    }

    @Test
    void null_estimated_departure_is_treated_as_on_schedule() {
        service.normaliseFlightStatus(new NormaliseFlightStatusCommand(
                "EK770", "DXB", "JFK", "on_time",
                "2026-05-23T06:00:00Z", null, "cirium"));

        FlightStatusEvent e = publisher.flightEvents.get(0);
        assertThat(e.estimatedDeparture()).isEqualTo(Instant.parse("2026-05-23T06:00:00Z"));
        assertThat(e.delay().toMinutes()).isZero();
    }

    @Test
    void unrecognised_status_string_maps_to_unknown() {
        service.normaliseFlightStatus(new NormaliseFlightStatusCommand(
                "QF001", "SYD", "LAX", "gate_change",
                "2026-05-23T22:00:00Z", "2026-05-23T22:00:00Z", "flightaware"));

        assertThat(publisher.flightEvents.get(0).status()).isEqualTo(FlightStatusCode.UNKNOWN);
    }

    @Test
    void weather_alert_normalised_and_published() {
        service.normaliseWeatherAlert(new NormaliseWeatherAlertCommand(
                "alert-001", "high", List.of("NBO", "MBA"),
                "Severe thunderstorms",
                "2026-05-23T12:00:00Z", "2026-05-23T18:00:00Z", "meteo"));

        assertThat(publisher.alertEvents).hasSize(1);
        WeatherAlertEvent e = publisher.alertEvents.get(0);
        assertThat(e.alertId()).isEqualTo("alert-001");
        assertThat(e.severity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(e.affectedAirports()).hasSize(2);
        assertThat(e.normalisedAt()).isEqualTo(NOW);
    }

    @Test
    void unrecognised_severity_falls_back_to_low() {
        service.normaliseWeatherAlert(new NormaliseWeatherAlertCommand(
                "alert-002", "extreme", List.of("MBA"),
                "Cyclone warning",
                "2026-05-23T10:00:00Z", "2026-05-24T10:00:00Z", "meteo"));

        assertThat(publisher.alertEvents.get(0).severity()).isEqualTo(AlertSeverity.LOW);
    }

    private static final class FakePublisher implements FeedEventPublisher {
        final List<FlightStatusEvent> flightEvents = new ArrayList<>();
        final List<WeatherAlertEvent> alertEvents = new ArrayList<>();

        @Override
        public void publishFlightStatus(FlightStatusEvent event) {
            flightEvents.add(event);
        }

        @Override
        public void publishWeatherAlert(WeatherAlertEvent event) {
            alertEvents.add(event);
        }
    }
}
