package com.resilire.partner.application;

import com.resilire.partner.domain.ReferralStatus;
import com.resilire.partner.infrastructure.persistence.ReferralJpaRepository;
import com.resilire.partner.infrastructure.persistence.ReferralRecord;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralService {

    private final ReferralJpaRepository referrals;

    public ReferralService(ReferralJpaRepository referrals) {
        this.referrals = referrals;
    }

    @Transactional
    public ReferralRecord createReferral(String referrerId, String referredId) {
        if (referrals.existsByReferrerIdAndReferredId(referrerId, referredId)) {
            return referrals.findByReferrerId(referrerId).stream()
                    .filter(r -> r.getReferredId().equals(referredId))
                    .findFirst()
                    .orElseThrow();
        }
        ReferralRecord r = new ReferralRecord();
        r.setId(UUID.randomUUID().toString());
        r.setReferrerId(referrerId);
        r.setReferredId(referredId);
        r.setStatus(ReferralStatus.PENDING);
        r.setCreatedAt(Instant.now());
        return referrals.save(r);
    }

    @Transactional
    public ReferralRecord creditReferral(String referralId) {
        ReferralRecord r = referrals.findById(referralId)
                .orElseThrow(() -> new NoSuchElementException("Referral not found: " + referralId));
        if (r.getStatus() != ReferralStatus.PENDING) {
            throw new IllegalStateException("Only PENDING referrals can be credited");
        }
        r.setStatus(ReferralStatus.CREDITED);
        r.setCreditedAt(Instant.now());
        return referrals.save(r);
    }

    @Transactional(readOnly = true)
    public List<ReferralRecord> findByReferrer(String referrerId) {
        return referrals.findByReferrerId(referrerId);
    }
}
