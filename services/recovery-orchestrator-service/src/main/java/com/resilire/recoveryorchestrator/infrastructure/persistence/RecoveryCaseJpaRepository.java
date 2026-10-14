package com.resilire.recoveryorchestrator.infrastructure.persistence;

import com.resilire.recoveryorchestrator.domain.RecoveryCaseStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryCaseJpaRepository extends JpaRepository<RecoveryCaseRecord, String> {
    Optional<RecoveryCaseRecord> findByDisruptionId(String disruptionId);
    List<RecoveryCaseRecord> findByStatus(RecoveryCaseStatus status);
}
