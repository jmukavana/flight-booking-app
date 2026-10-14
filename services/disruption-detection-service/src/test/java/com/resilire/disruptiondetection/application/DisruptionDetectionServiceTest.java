package com.resilire.disruptiondetection.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.resilire.disruptiondetection.domain.disruption.Disruption;
import com.resilire.disruptiondetection.domain.disruption.DisruptionCause;
import com.resilire.disruptiondetection.domain.disruption.DisruptionDomainEvent;
import com.resilire.disruptiondetection.domain.disruption.DisruptionId;
import com.resilire.disruptiondetection.domain.disruption.DisruptionRepository;
import com.resilire.disruptiondetection.domain.disruption.DisruptionSeverity;
import com.resilire.disruptiondetection.domain.disruption.DisruptionStatus;
import com.resilire.disruptiondetection.domain.disruption.FlightRef;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DisruptionDetectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-23T10:00:00Z");

    private InMemoryDisruptionRepository repository;
    private FakePublisher publisher;
    private DisruptionDetectionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDisruptionRepository();
        publisher = new FakePublisher();
        service = new DisruptionDetectionService(repository, publisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void cancelled_flight_creates_active_disruption_and_emits_event() {
        service.onFlightStatusUpdated(flightStatus("KQ101", "CANCELLED", 0));

        assertThat(repository.disruptions).hasSize(1);
        Disruption d = repository.disruptions.values().iterator().next();
        assertThat(d.status()).isEqualTo(DisruptionStatus.ACTIVE);
        assertThat(d.cause()).isEqualTo(DisruptionCause.FLIGHT_CANCELLED);
        assertThat(d.severity()).isEqualTo(DisruptionSeverity.CANCELLED);

        assertThat(publisher.events).singleElement()
                .isInstanceOf(DisruptionDomainEvent.DisruptionDetected.class);
    }

    @Test
    void delay_below_threshold_does_not_create_disruption() {
        service.onFlightStatusUpdated(flightStatus("KQ102", "DELAYED", 30));

        assertThat(repository.disruptions).isEmpty();
        assertThat(publisher.events).isEmpty();
    }

    @Test
    void delay_at_threshold_creates_moderate_disruption() {
        service.onFlightStatusUpdated(flightStatus("KQ103", "DELAYED", 45));

        assertThat(repository.disruptions).hasSize(1);
        Disruption d = repository.disruptions.values().iterator().next();
        assertThat(d.severity()).isEqualTo(DisruptionSeverity.MODERATE);
        assertThat(d.cause()).isEqualTo(DisruptionCause.SIGNIFICANT_DELAY);
    }

    @Test
    void severe_delay_creates_severe_disruption() {
        service.onFlightStatusUpdated(flightStatus("KQ104", "DELAYED", 150));

        Disruption d = repository.disruptions.values().iterator().next();
        assertThat(d.severity()).isEqualTo(DisruptionSeverity.SEVERE);
    }

    @Test
    void second_status_update_does_not_create_duplicate_disruption() {
        service.onFlightStatusUpdated(flightStatus("KQ105", "DELAYED", 60));
        service.onFlightStatusUpdated(flightStatus("KQ105", "DELAYED", 90));

        assertThat(repository.disruptions).hasSize(1);
        assertThat(publisher.events).hasSize(1);
        assertThat(repository.disruptions.values().iterator().next().delayMinutes()).isEqualTo(90);
    }

    @Test
    void diverted_flight_creates_diversion_disruption() {
        service.onFlightStatusUpdated(flightStatus("KQ106", "DIVERTED", 0));

        Disruption d = repository.disruptions.values().iterator().next();
        assertThat(d.cause()).isEqualTo(DisruptionCause.DIVERSION);
    }

    @Test
    void landed_flight_resolves_active_disruption_and_emits_event() {
        service.onFlightStatusUpdated(flightStatus("KQ107", "DELAYED", 60));
        publisher.events.clear();

        service.onFlightStatusUpdated(flightStatus("KQ107", "LANDED", 0));

        Disruption d = repository.disruptions.values().iterator().next();
        assertThat(d.status()).isEqualTo(DisruptionStatus.RESOLVED);
        assertThat(d.resolvedAt()).isEqualTo(NOW);

        assertThat(publisher.events).singleElement()
                .isInstanceOf(DisruptionDomainEvent.DisruptionResolved.class);
    }

    @Test
    void landed_flight_without_prior_disruption_does_not_emit_event() {
        service.onFlightStatusUpdated(flightStatus("KQ108", "LANDED", 0));

        assertThat(repository.disruptions).isEmpty();
        assertThat(publisher.events).isEmpty();
    }

    private FlightStatusProjection flightStatus(String flightId, String status, long delay) {
        return new FlightStatusProjection("evt-1", flightId, "NBO", "LHR", status, delay);
    }

    private static final class InMemoryDisruptionRepository implements DisruptionRepository {
        private final Map<String, Disruption> disruptions = new HashMap<>();

        @Override
        public void save(Disruption disruption) {
            disruptions.put(disruption.id().value(), disruption);
        }

        @Override
        public Optional<Disruption> findActiveByFlightId(String flightId) {
            return disruptions.values().stream()
                    .filter(d -> d.flight().flightId().equals(flightId)
                            && d.status() == DisruptionStatus.ACTIVE)
                    .findFirst();
        }

        @Override
        public List<Disruption> findAllActive() {
            return disruptions.values().stream()
                    .filter(d -> d.status() == DisruptionStatus.ACTIVE)
                    .toList();
        }

        @Override
        public Optional<Disruption> findById(DisruptionId id) {
            return Optional.ofNullable(disruptions.get(id.value()));
        }
    }

    private static final class FakePublisher implements DisruptionEventPublisher {
        private final List<DisruptionDomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DisruptionDomainEvent event) {
            events.add(event);
        }
    }
}
