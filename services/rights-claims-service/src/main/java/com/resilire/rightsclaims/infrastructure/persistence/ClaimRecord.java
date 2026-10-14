package com.resilire.rightsclaims.infrastructure.persistence;

import com.resilire.rightsclaims.domain.ClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "claims", indexes = {
        @Index(name = "idx_claim_traveller", columnList = "traveller_id"),
        @Index(name = "idx_claim_assessment", columnList = "assessment_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ClaimRecord {

    @Id
    private String id;

    @Column(name = "assessment_id", nullable = false)
    private String assessmentId;

    @Column(name = "traveller_id", nullable = false)
    private String travellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "payment_ref")
    private String paymentRef;

    @Column(length = 1024)
    private String notes;
}
