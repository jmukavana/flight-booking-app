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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the EU261/2004 compensation grid on first startup if no active EU261
 * ruleset exists. The ruleset is data — the engine is jurisdiction-agnostic.
 * Rules can be updated via the admin API without touching code.
 *
 * Compensation amounts (EU261 Art.7, distance measured as great-circle):
 * ≤ 1500 km, controllable, delay ≥ 3h at destination → €250
 * 1500–3500 km, controllable, delay ≥ 3h → €400
 * > 3500 km, controllable, delay 3–4h → €300
 * > 3500 km, controllable, delay > 4h → €600
 * Cancellation (any distance), controllable → mandatory refund
 */
@Component
class Eu261RulesetInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Eu261RulesetInitializer.class);

    private final RulesetJpaRepository rulesets;
    private final EligibilityRuleJpaRepository rules;

    Eu261RulesetInitializer(RulesetJpaRepository rulesets, EligibilityRuleJpaRepository rules) {
        this.rulesets = rulesets;
        this.rules = rules;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (rulesets.existsByJurisdictionAndStatus(Jurisdiction.EU261, RulesetStatus.ACTIVE)) {
            return;
        }

        log.info("Seeding EU261 ruleset v2024.1");

        RulesetRecord ruleset = new RulesetRecord();
        ruleset.setId(UUID.randomUUID().toString());
        ruleset.setJurisdiction(Jurisdiction.EU261);
        ruleset.setVersion("2024.1");
        ruleset.setStatus(RulesetStatus.ACTIVE);
        ruleset.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        ruleset.setCreatedAt(Instant.now());
        rulesets.save(ruleset);

        String rid = ruleset.getId();
        rules.saveAll(List.of(
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, null, null, 1500, new BigDecimal("250.00"), "EUR",
                        "EU261 Art.7: ≤1500 km, delay ≥3h, controllable → €250"),
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, null, 1501, 3500, new BigDecimal("400.00"), "EUR",
                        "EU261 Art.7: 1500–3500 km, delay ≥3h, controllable → €400"),
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, 239, 3501, null, new BigDecimal("300.00"), "EUR",
                        "EU261 Art.7: >3500 km, delay 3–4h, controllable → €300"),
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        240, null, 3501, null, new BigDecimal("600.00"), "EUR",
                        "EU261 Art.7: >3500 km, delay >4h, controllable → €600"),
                rule(rid, EntitlementType.MANDATORY_REFUND, DisruptionCause.CONTROLLABLE,
                        0, null, null, null, null, null,
                        "EU261 Art.8: cancellation (any distance), controllable → mandatory refund")));

        log.info("EU261 ruleset seeded: {} with {} rules", ruleset.getId(), 5);
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
