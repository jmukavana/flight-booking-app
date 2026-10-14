package com.resilire.rightsclaims.infrastructure.persistence;

import com.resilire.rightsclaims.domain.ClaimStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimJpaRepository extends JpaRepository<ClaimRecord, String> {
    List<ClaimRecord> findByTravellerId(String travellerId);
    List<ClaimRecord> findByAssessmentId(String assessmentId);
    boolean existsByAssessmentIdAndStatus(String assessmentId, ClaimStatus status);
}
