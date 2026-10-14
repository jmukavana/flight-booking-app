package com.resilire.partner.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralJpaRepository extends JpaRepository<ReferralRecord, String> {
    List<ReferralRecord> findByReferrerId(String referrerId);
    boolean existsByReferrerIdAndReferredId(String referrerId, String referredId);
}
