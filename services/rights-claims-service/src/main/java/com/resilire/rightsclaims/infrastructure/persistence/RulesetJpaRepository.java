package com.resilire.rightsclaims.infrastructure.persistence;

import com.resilire.rightsclaims.domain.Jurisdiction;
import com.resilire.rightsclaims.domain.RulesetStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RulesetJpaRepository extends JpaRepository<RulesetRecord, String> {
    Optional<RulesetRecord> findByJurisdictionAndStatus(Jurisdiction jurisdiction, RulesetStatus status);
    boolean existsByJurisdictionAndStatus(Jurisdiction jurisdiction, RulesetStatus status);
    List<RulesetRecord> findByJurisdiction(Jurisdiction jurisdiction);
    List<RulesetRecord> findByStatusAndEffectiveFromLessThanEqual(RulesetStatus status, LocalDate date);
}
