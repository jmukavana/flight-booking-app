package com.resilire.disruptiondetection.infrastructure.persistence;

import com.resilire.disruptiondetection.domain.disruption.Disruption;
import com.resilire.disruptiondetection.domain.disruption.DisruptionCause;
import com.resilire.disruptiondetection.domain.disruption.DisruptionId;
import com.resilire.disruptiondetection.domain.disruption.DisruptionRepository;
import com.resilire.disruptiondetection.domain.disruption.DisruptionSeverity;
import com.resilire.disruptiondetection.domain.disruption.DisruptionStatus;
import com.resilire.disruptiondetection.domain.disruption.FlightRef;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaDisruptionRepository implements DisruptionRepository {

    private final DisruptionJpaRepository jpa;

    JpaDisruptionRepository(DisruptionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Disruption disruption) {
        jpa.save(toRecord(disruption));
    }

    @Override
    public Optional<Disruption> findActiveByFlightId(String flightId) {
        return jpa.findByFlightIdAndStatus(flightId, DisruptionStatus.ACTIVE.name())
                .map(this::toDomain);
    }

    @Override
    public List<Disruption> findAllActive() {
        return jpa.findByStatus(DisruptionStatus.ACTIVE.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Disruption> findById(DisruptionId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    private DisruptionRecord toRecord(Disruption d) {
        DisruptionRecord r = new DisruptionRecord();
        r.setId(d.id().value());
        r.setFlightId(d.flight().flightId());
        r.setOrigin(d.flight().origin());
        r.setDestination(d.flight().destination());
        r.setStatus(d.status().name());
        r.setCause(d.cause().name());
        r.setSeverity(d.severity().name());
        r.setDelayMinutes(d.delayMinutes());
        r.setDetectedAt(d.detectedAt());
        r.setResolvedAt(d.resolvedAt());
        r.setLastUpdatedAt(d.lastUpdatedAt());
        return r;
    }

    private Disruption toDomain(DisruptionRecord r) {
        return Disruption.reconstitute(
                DisruptionId.of(r.getId()),
                new FlightRef(r.getFlightId(), r.getOrigin(), r.getDestination()),
                DisruptionStatus.valueOf(r.getStatus()),
                DisruptionCause.valueOf(r.getCause()),
                DisruptionSeverity.valueOf(r.getSeverity()),
                r.getDelayMinutes(),
                r.getDetectedAt(),
                r.getResolvedAt(),
                r.getLastUpdatedAt());
    }
}
