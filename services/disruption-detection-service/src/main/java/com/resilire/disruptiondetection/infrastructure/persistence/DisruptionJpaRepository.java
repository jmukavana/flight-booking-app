package com.resilire.disruptiondetection.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface DisruptionJpaRepository extends JpaRepository<DisruptionRecord, String> {
    Optional<DisruptionRecord> findByFlightIdAndStatus(String flightId, String status);
    List<DisruptionRecord> findByStatus(String status);
}
