package com.resilire.rightsclaims.infrastructure.seed;

import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.EntitlementType;
import com.resilire.rightsclaims.domain.Jurisdiction;
import com.resilire.rightsclaims.domain.RulesetStatus;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleRecord;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds Canada APPR (Air Passenger Protection Regulations, 2019) for large carriers
 * (> 180,000 passengers/year). Amounts are in CAD. Phase 3 jurisdiction expansion.
 *
 * Compensation grid (APPR Schedule 1, large carrier):
 * ≤ 1500 km, controllable, delay 3h–5h59m → CAD $400
 * ≤ 1500 km, controllable, delay ≥ 6h → CAD $700
 * 1500–3500 km, controllable, delay ≥ 3h → CAD $600
 * > 3500 km, controllable, delay ≥ 3h → CAD $1000
 * Cancellation, controllable → CAD $1000 + mandatory refund
 */
@Component
@Order(3)
class CanadaApprRulesetInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CanadaApprRulesetInitializer.class);

    private final RulesetJpaRepository rulesets;
    private final EligibilityRuleJpaRepository rules;

    CanadaApprRulesetInitializer(RulesetJpaRepository rulesets, EligibilityRuleJpaRepository rules) {
        this.rulesets = rulesets;
        this.rules = rules;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (rulesets.existsByJurisdictionAndStatus(Jurisdiction.CANADA_APPR, RulesetStatus.ACTIVE)) {
            return;
        }

        log.info("Seeding CANADA_APPR ruleset v2024.1");

        RulesetRecord ruleset = new RulesetRecord();
        ruleset.setId(UUID.randomUUID().toString());
        ruleset.setJurisdiction(Jurisdiction.CANADA_APPR);
        ruleset.setVersion("2024.1");
        ruleset.setStatus(RulesetStatus.ACTIVE);
        ruleset.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        ruleset.setCreatedAt(Instant.now());
        rulesets.save(ruleset);

        String rid = ruleset.getId();
        rules.saveAll(List.of(
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, 359, null, 1500, new BigDecimal("400.00"), "CAD",
                        "APPR: ≤1500 km, delay 3–6h, controllable → CAD$400"),
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        360, null, null, 1500, new BigDecimal("700.00"), "CAD",
                        "APPR: ≤1500 km, delay ≥6h, controllable → CAD$700"),
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, null, 1501, 3500, new BigDecimal("600.00"), "CAD",
                        "APPR: 1500–3500 km, delay ≥3h, controllable → CAD$600"),
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, null, 3501, null, new BigDecimal("1000.00"), "CAD",
                        "APPR: >3500 km, delay ≥3h, controllable → CAD$1000"),
                rule(rid, EntitlementType.MANDATORY_REFUND, DisruptionCause.CONTROLLABLE,
                        0, null, null, null, new BigDecimal("1000.00"), "CAD",
                        "APPR: cancellation, controllable → CAD$1000 + mandatory refund")));

        log.info("CANADA_APPR ruleset seeded: {} with 5 rules", ruleset.getId());
    }

    private EligibilityRuleRecord rule(String rulesetId, EntitlementType type, DisruptionCause cause,
                                       int delayMin, Integer delayMax,
                                       Integer distMin, Integer distMax,
                                       BigDecimal amount, String currency, String description) {
        EligibilityRuleRecord r = new EligibilityRuleRecord();
        r.setId(UUID.randomUUID().toString());
        r.setRulesetId(rulesetId);
        r.setEntitlementType(type);
        r.setCause(cause);
        r.setDelayMinMinutes(delayMin);
        r.setDelayMaxMinutes(delayMax);
        r.setFlightDistanceKmMin(distMin);
        r.setFlightDistanceKmMax(distMax);
        r.setCompensationAmount(amount);
        r.setCurrency(currency);
        r.setDescription(description);
        return r;
    }
}
