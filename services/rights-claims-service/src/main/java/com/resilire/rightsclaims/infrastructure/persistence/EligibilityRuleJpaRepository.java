package com.resilire.rightsclaims.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibilityRuleJpaRepository extends JpaRepository<EligibilityRuleRecord, String> {
    List<EligibilityRuleRecord> findByRulesetId(String rulesetId);
}
