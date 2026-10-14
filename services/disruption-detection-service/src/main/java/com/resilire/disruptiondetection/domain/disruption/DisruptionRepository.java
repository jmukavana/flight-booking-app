package com.resilire.disruptiondetection.domain.disruption;

import java.util.List;
import java.util.Optional;

public interface DisruptionRepository {
    void save(Disruption disruption);
    Optional<Disruption> findActiveByFlightId(String flightId);
    List<Disruption> findAllActive();
    Optional<Disruption> findById(DisruptionId id);
}
