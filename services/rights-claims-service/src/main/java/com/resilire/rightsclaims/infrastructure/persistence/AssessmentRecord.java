package com.resilire.rightsclaims.infrastructure.persistence;

import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.EntitlementType;
import com.resilire.rightsclaims.domain.Jurisdiction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "eligibility_assessments", indexes = {
        @Index(name = "idx_assessment_disruption", columnList = "disruption_id"),
        @Index(name = "idx_assessment_flight", columnList = "flight_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AssessmentRecord {

    @Id
    private String id;

    @Column(name = "disruption_id")
    private String disruptionId;

    @Column(name = "flight_id", nullable = false)
    private String flightId;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Jurisdiction jurisdiction;

    @Column(name = "delay_minutes", nullable = false)
    private int delayMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisruptionCause cause;

    @Column(name = "flight_distance_km")
    private Integer flightDistanceKm;

    @Column(name = "ruleset_id")
    private String rulesetId;

    @Column(name = "ruleset_version")
    private String rulesetVersion;

    @Column(nullable = false)
    private boolean eligible;

    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_type")
    private EntitlementType entitlementType;

    @Column(name = "compensation_amount", precision = 10, scale = 2)
    private BigDecimal compensationAmount;

    @Column(length = 3)
    private String currency;

    @Column(length = 1024)
    private String reasoning;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;
}
