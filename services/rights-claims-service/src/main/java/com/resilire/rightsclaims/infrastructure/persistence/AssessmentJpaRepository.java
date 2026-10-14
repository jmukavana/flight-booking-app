package com.resilire.rightsclaims.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentJpaRepository extends JpaRepository<AssessmentRecord, String> {
    Optional<AssessmentRecord> findByDisruptionId(String disruptionId);
}
