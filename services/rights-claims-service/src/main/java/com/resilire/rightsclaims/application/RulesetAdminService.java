package com.resilire.rightsclaims.application;

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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 4 ruleset governance service. Rules are data — compliance officers manage
 * rulesets through this API without code deployments (ADR-4). Future-dated rulesets
 * are activated automatically by {@link #activateDue()}, enabling the EU261-trilogue
 * hedge: publish a future-dated ruleset before it is legally live.
 */
@Service
public class RulesetAdminService {

    private static final Logger log = LoggerFactory.getLogger(RulesetAdminService.class);

    private final RulesetJpaRepository rulesets;
    private final EligibilityRuleJpaRepository rules;

    public RulesetAdminService(RulesetJpaRepository rulesets, EligibilityRuleJpaRepository rules) {
        this.rulesets = rulesets;
        this.rules = rules;
    }

    @Transactional
    public RulesetRecord createDraft(Jurisdiction jurisdiction, String version, LocalDate effectiveFrom) {
        RulesetRecord r = new RulesetRecord();
        r.setId(UUID.randomUUID().toString());
        r.setJurisdiction(jurisdiction);
        r.setVersion(version);
        r.setStatus(RulesetStatus.DRAFT);
        r.setEffectiveFrom(effectiveFrom);
        r.setCreatedAt(Instant.now());
        return rulesets.save(r);
    }

    @Transactional
    public EligibilityRuleRecord addRule(String rulesetId, EntitlementType entitlementType,
                                         DisruptionCause cause, int delayMinMinutes,
                                         Integer delayMaxMinutes, Integer distanceKmMin,
                                         Integer distanceKmMax, BigDecimal compensationAmount,
                                         String currency, String description) {
        RulesetRecord ruleset = rulesets.findById(rulesetId)
                .orElseThrow(() -> new NoSuchElementException("Ruleset not found: " + rulesetId));
        if (ruleset.getStatus() != RulesetStatus.DRAFT) {
            throw new IllegalStateException("Rules can only be added to DRAFT rulesets");
        }
        EligibilityRuleRecord rule = new EligibilityRuleRecord();
        rule.setId(UUID.randomUUID().toString());
        rule.setRulesetId(rulesetId);
        rule.setEntitlementType(entitlementType);
        rule.setCause(cause);
        rule.setDelayMinMinutes(delayMinMinutes);
        rule.setDelayMaxMinutes(delayMaxMinutes);
        rule.setFlightDistanceKmMin(distanceKmMin);
        rule.setFlightDistanceKmMax(distanceKmMax);
        rule.setCompensationAmount(compensationAmount);
        rule.setCurrency(currency);
        rule.setDescription(description);
        return rules.save(rule);
    }

    @Transactional
    public RulesetRecord activate(String rulesetId) {
        RulesetRecord toActivate = rulesets.findById(rulesetId)
                .orElseThrow(() -> new NoSuchElementException("Ruleset not found: " + rulesetId));
        if (toActivate.getStatus() != RulesetStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT rulesets can be activated");
        }
        rulesets.findByJurisdictionAndStatus(toActivate.getJurisdiction(), RulesetStatus.ACTIVE)
                .ifPresent(current -> {
                    current.setStatus(RulesetStatus.SUPERSEDED);
                    rulesets.save(current);
                });
        toActivate.setStatus(RulesetStatus.ACTIVE);
        log.info("Activated ruleset {} ({} v{})", rulesetId,
                toActivate.getJurisdiction(), toActivate.getVersion());
        return rulesets.save(toActivate);
    }

    @Transactional
    public RulesetRecord supersede(String rulesetId) {
        RulesetRecord r = rulesets.findById(rulesetId)
                .orElseThrow(() -> new NoSuchElementException("Ruleset not found: " + rulesetId));
        if (r.getStatus() != RulesetStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE rulesets can be superseded");
        }
        r.setStatus(RulesetStatus.SUPERSEDED);
        return rulesets.save(r);
    }

    @Transactional(readOnly = true)
    public List<RulesetRecord> findAll(Jurisdiction jurisdiction) {
        return rulesets.findByJurisdiction(jurisdiction);
    }

    @Transactional(readOnly = true)
    public Optional<RulesetRecord> findById(String id) {
        return rulesets.findById(id);
    }

    @Transactional(readOnly = true)
    public List<EligibilityRuleRecord> findRules(String rulesetId) {
        return rules.findByRulesetId(rulesetId);
    }

    /** Hourly: auto-activate DRAFT rulesets whose effectiveFrom date has arrived. */
    @Scheduled(fixedRateString = "${resilire.governance.activation-interval-ms:3600000}")
    @Transactional
    public void activateDue() {
        List<RulesetRecord> due = rulesets.findByStatusAndEffectiveFromLessThanEqual(
                RulesetStatus.DRAFT, LocalDate.now());
        if (!due.isEmpty()) {
            log.info("Auto-activating {} due ruleset(s)", due.size());
        }
        due.forEach(r -> activate(r.getId()));
    }
}
