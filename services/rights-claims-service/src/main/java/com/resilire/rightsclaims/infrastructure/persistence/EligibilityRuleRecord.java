package com.resilire.rightsclaims.infrastructure.persistence;

import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.EntitlementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single eligibility rule within a ruleset. Rules are purely data — the engine
 * matches them against a disruption context without any hard-coded jurisdiction logic.
 * Null bounds mean "no limit" (e.g. null delayMaxMinutes = no upper delay cap).
 */
@Entity
@Table(name = "eligibility_rules")
@Getter
@Setter
@NoArgsConstructor
public class EligibilityRuleRecord {

    @Id
    private String id;

    @Column(name = "ruleset_id", nullable = false)
    private String rulesetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_type", nullable = false)
    private EntitlementType entitlementType;

    /** CONTROLLABLE, UNCONTROLLABLE, or null (matches any cause). */
    @Enumerated(EnumType.STRING)
    @Column(name = "cause")
    private DisruptionCause cause;

    @Column(name = "delay_min_minutes", nullable = false)
    private int delayMinMinutes;

    @Column(name = "delay_max_minutes")
    private Integer delayMaxMinutes;

    @Column(name = "flight_distance_km_min")
    private Integer flightDistanceKmMin;

    @Column(name = "flight_distance_km_max")
    private Integer flightDistanceKmMax;

    @Column(name = "compensation_amount", precision = 10, scale = 2)
    private BigDecimal compensationAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "description", length = 512)
    private String description;
}
