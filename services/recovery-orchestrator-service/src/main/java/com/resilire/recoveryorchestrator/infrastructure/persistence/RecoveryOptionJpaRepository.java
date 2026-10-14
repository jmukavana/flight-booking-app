package com.resilire.recoveryorchestrator.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryOptionJpaRepository extends JpaRepository<RecoveryOptionRecord, String> {
    List<RecoveryOptionRecord> findByCaseId(String caseId);
}
