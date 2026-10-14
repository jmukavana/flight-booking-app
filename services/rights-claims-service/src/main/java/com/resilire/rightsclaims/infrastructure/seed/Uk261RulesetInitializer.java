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
 * Seeds the UK261/2004 compensation grid (Phase 3 jurisdiction expansion).
 * UK261 carries EU Regulation 261/2004 into UK law via the European Union
 * (Withdrawal) Act 2018. Amounts are expressed in GBP at the Brexit conversion.
 *
 * Compensation grid (same tier structure as EU261, GBP amounts):
 * ≤ 1500 km, controllable, delay ≥ 3h → £220
 * 1500–3500 km, controllable, delay ≥ 3h → £350
 * > 3500 km, controllable, delay 3–4h → £260
 * > 3500 km, controllable, delay > 4h → £520
 */
@Component
@Order(2)
class Uk261RulesetInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Uk261RulesetInitializer.class);

    private final RulesetJpaRepository rulesets;
    private final EligibilityRuleJpaRepository rules;

    Uk261RulesetInitializer(RulesetJpaRepository rulesets, EligibilityRuleJpaRepository rules) {
        this.rulesets = rulesets;
        this.rules = rules;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (rulesets.existsByJurisdictionAndStatus(Jurisdiction.UK261, RulesetStatus.ACTIVE)) {
            return;
        }

        log.info("Seeding UK261 ruleset v2024.1");

        RulesetRecord ruleset = new RulesetRecord();
        ruleset.setId(UUID.randomUUID().toString());
        ruleset.setJurisdiction(Jurisdiction.UK261);
        ruleset.setVersion("2024.1");
        ruleset.setStatus(RulesetStatus.ACTIVE);
        ruleset.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        ruleset.setCreatedAt(Instant.now());
        rulesets.save(ruleset);

        String rid = ruleset.getId();
        rules.saveAll(List.of(
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, null, null, 1500, new BigDecimal("220.00"), "GBP",
                        "UK261: ≤1500 km, delay ≥3h, controllable → £220"),
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, null, 1501, 3500, new BigDecimal("350.00"), "GBP",
                        "UK261: 1500–3500 km, delay ≥3h, controllable → £350"),
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        180, 239, 3501, null, new BigDecimal("260.00"), "GBP",
                        "UK261: >3500 km, delay 3–4h, controllable → £260"),
                rule(rid, EntitlementType.COMPENSATION, DisruptionCause.CONTROLLABLE,
                        240, null, 3501, null, new BigDecimal("520.00"), "GBP",
                        "UK261: >3500 km, delay >4h, controllable → £520"),
                rule(rid, EntitlementType.MANDATORY_REFUND, DisruptionCause.CONTROLLABLE,
                        0, null, null, null, null, null,
                        "UK261: cancellation (any distance), controllable → mandatory refund")));

        log.info("UK261 ruleset seeded: {} with 5 rules", ruleset.getId());
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
