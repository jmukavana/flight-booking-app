package com.resilire.rightsclaims.application;

import com.resilire.rightsclaims.domain.ClaimStatus;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentRecord;
import com.resilire.rightsclaims.infrastructure.persistence.ClaimJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.ClaimRecord;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimApplicationService {

    private final ClaimJpaRepository claims;
    private final AssessmentJpaRepository assessments;
    private final ClaimEventPublisher events;

    public ClaimApplicationService(ClaimJpaRepository claims, AssessmentJpaRepository assessments,
                                   ClaimEventPublisher events) {
        this.claims = claims;
        this.assessments = assessments;
        this.events = events;
    }

    @Transactional
    public ClaimRecord submit(String assessmentId, String travellerId) {
        AssessmentRecord assessment = assessments.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        if (!assessment.isEligible()) {
            throw new IllegalStateException("Assessment " + assessmentId + " found traveller ineligible");
        }

        if (claims.existsByAssessmentIdAndStatus(assessmentId, ClaimStatus.SUBMITTED)) {
            return claims.findByAssessmentId(assessmentId).stream()
                    .filter(c -> c.getStatus() == ClaimStatus.SUBMITTED)
                    .findFirst()
                    .orElseThrow();
        }

        ClaimRecord claim = new ClaimRecord();
        claim.setId(UUID.randomUUID().toString());
        claim.setAssessmentId(assessmentId);
        claim.setTravellerId(travellerId);
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setSubmittedAt(Instant.now());
        claims.save(claim);
        events.publishClaimSubmitted(claim);
        return claim;
    }

    @Transactional
    public ClaimRecord approve(String claimId, String notes) {
        ClaimRecord claim = loadClaim(claimId);
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setResolvedAt(Instant.now());
        claim.setNotes(notes);
        claims.save(claim);
        events.publishClaimApproved(claim);
        return claim;
    }

    @Transactional
    public ClaimRecord reject(String claimId, String reason) {
        ClaimRecord claim = loadClaim(claimId);
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setResolvedAt(Instant.now());
        claim.setNotes(reason);
        return claims.save(claim);
    }

    @Transactional
    public ClaimRecord markPaid(String claimId, String paymentRef) {
        ClaimRecord claim = loadClaim(claimId);
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED claims can be marked PAID");
        }
        claim.setStatus(ClaimStatus.PAID);
        claim.setPaymentRef(paymentRef);
        claim.setResolvedAt(Instant.now());
        ClaimRecord saved = claims.save(claim);
        events.publishClaimPaid(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<ClaimRecord> findById(String id) {
        return claims.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ClaimRecord> findByTraveller(String travellerId) {
        return claims.findByTravellerId(travellerId);
    }

    private ClaimRecord loadClaim(String claimId) {
        return claims.findById(claimId)
                .orElseThrow(() -> new NoSuchElementException("Claim not found: " + claimId));
    }
}
