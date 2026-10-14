package com.resilire.disruptiondetection.application;

import com.resilire.disruptiondetection.domain.disruption.Disruption;
import com.resilire.disruptiondetection.domain.disruption.DisruptionCause;
import com.resilire.disruptiondetection.domain.disruption.DisruptionDomainEvent;
import com.resilire.disruptiondetection.domain.disruption.DisruptionId;
import com.resilire.disruptiondetection.domain.disruption.DisruptionRepository;
import com.resilire.disruptiondetection.domain.disruption.FlightRef;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the disruption state machine. Each flight carries at most one ACTIVE
 * disruption episode at a time. When a new disruptive status arrives it creates
 * an episode (emitting {@code disruption.detected}); when the flight lands it
 * closes it (emitting {@code disruption.resolved}).
 *
 * <p>The early-warning threshold is 45 minutes — earlier than EU261's 3-hour
 * destination threshold — so travellers get actionable notice while there is
 * still time to rebook.
 */
@Service
public class DisruptionDetectionService {

    private final DisruptionRepository repository;
    private final DisruptionEventPublisher publisher;
    private final Clock clock;

    public DisruptionDetectionService(DisruptionRepository repository,
                                      DisruptionEventPublisher publisher) {
        this(repository, publisher, Clock.systemUTC());
    }

    DisruptionDetectionService(DisruptionRepository repository,
                               DisruptionEventPublisher publisher, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public void onFlightStatusUpdated(FlightStatusProjection projection) {
        String flightId = projection.flightId();
        String status = projection.status();
        long delayMinutes = projection.delayMinutes();
        Instant now = Instant.now(clock);

        Optional<Disruption> existing = repository.findActiveByFlightId(flightId);

        if (isDisruptive(status, delayMinutes)) {
            if (existing.isEmpty()) {
                FlightRef ref = new FlightRef(flightId, projection.origin(), projection.destination());
                DisruptionCause cause = toCause(status);
                Disruption disruption = Disruption.detect(
                        DisruptionId.newId(), ref, cause, (int) delayMinutes, now);
                repository.save(disruption);
                publishAll(disruption.pullDomainEvents());
            } else {
                existing.get().updateDelay((int) delayMinutes, now);
                repository.save(existing.get());
            }
        } else if ("LANDED".equalsIgnoreCase(status) && existing.isPresent()) {
            existing.get().resolve(now);
            repository.save(existing.get());
            publishAll(existing.get().pullDomainEvents());
        }
    }

    @Transactional(readOnly = true)
    public List<Disruption> activeDisruptions() {
        return repository.findAllActive();
    }

    private void publishAll(List<DisruptionDomainEvent> events) {
        events.forEach(publisher::publish);
    }

    private boolean isDisruptive(String status, long delayMinutes) {
        return "CANCELLED".equalsIgnoreCase(status)
                || "DIVERTED".equalsIgnoreCase(status)
                || ("DELAYED".equalsIgnoreCase(status) && delayMinutes >= Disruption.EARLY_WARNING_DELAY_MINUTES);
    }

    private DisruptionCause toCause(String status) {
        return switch (status.toUpperCase()) {
            case "CANCELLED" -> DisruptionCause.FLIGHT_CANCELLED;
            case "DIVERTED" -> DisruptionCause.DIVERSION;
            default -> DisruptionCause.SIGNIFICANT_DELAY;
        };
    }
}
