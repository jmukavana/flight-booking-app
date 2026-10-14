package com.resilire.rightsclaims.application;

import com.resilire.rightsclaims.domain.RulesetStatus;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentRecord;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleRecord;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetJpaRepository;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The eligibility engine. Rules are data — no jurisdiction logic is hard-coded.
 * The active ruleset for the disruption's jurisdiction is loaded, its rules are
 * evaluated against the disruption context, and the result (with reasoning) is
 * persisted for audit. Each disruptionId yields at most one assessment; duplicate
 * Kafka deliveries are silently idempotent.
 */
@Service
public class EligibilityService {

    private final RulesetJpaRepository rulesets;
    private final EligibilityRuleJpaRepository rules;
    private final AssessmentJpaRepository assessments;

    public EligibilityService(RulesetJpaRepository rulesets,
                              EligibilityRuleJpaRepository rules,
                              AssessmentJpaRepository assessments) {
        this.rulesets = rulesets;
        this.rules = rules;
        this.assessments = assessments;
    }

    @Transactional
    public AssessmentRecord assess(AssessmentRequest request) {
        if (request.disruptionId() != null) {
            Optional<AssessmentRecord> existing = assessments.findByDisruptionId(request.disruptionId());
            if (existing.isPresent()) return existing.get();
        }

        AssessmentRecord record = new AssessmentRecord();
        record.setId(UUID.randomUUID().toString());
        record.setDisruptionId(request.disruptionId());
        record.setFlightId(request.flightId());
        record.setOrigin(request.origin());
        record.setDestination(request.destination());
        record.setJurisdiction(request.jurisdiction());
        record.setDelayMinutes(request.delayMinutes());
        record.setCause(request.cause());
        record.setFlightDistanceKm(request.flightDistanceKm());
        record.setAssessedAt(Instant.now());

        Optional<RulesetRecord> activeRuleset =
                rulesets.findByJurisdictionAndStatus(request.jurisdiction(), RulesetStatus.ACTIVE);

        if (activeRuleset.isEmpty()) {
            record.setEligible(false);
            record.setReasoning("No active ruleset for jurisdiction: " + request.jurisdiction());
            return assessments.save(record);
        }

        RulesetRecord ruleset = activeRuleset.get();
        record.setRulesetId(ruleset.getId());
        record.setRulesetVersion(ruleset.getVersion());

        List<EligibilityRuleRecord> matching = rules.findByRulesetId(ruleset.getId()).stream()
                .filter(r -> matchesCause(r, request))
                .filter(r -> matchesDelay(r, request))
                .filter(r -> matchesDistance(r, request))
                .toList();

        if (matching.isEmpty()) {
            record.setEligible(false);
            record.setReasoning("No rule matches: delay=" + request.delayMinutes()
                    + "min, cause=" + request.cause()
                    + ", distanceKm=" + request.flightDistanceKm()
                    + " (ruleset " + ruleset.getVersion() + ")");
        } else {
            EligibilityRuleRecord rule = matching.get(0);
            record.setEligible(true);
            record.setEntitlementType(rule.getEntitlementType());
            record.setCompensationAmount(rule.getCompensationAmount());
            record.setCurrency(rule.getCurrency());
            record.setReasoning(rule.getDescription()
                    + " — ruleset " + ruleset.getJurisdiction() + " v" + ruleset.getVersion());
        }

        return assessments.save(record);
    }

    @Transactional(readOnly = true)
    public Optional<AssessmentRecord> findById(String id) {
        return assessments.findById(id);
    }

    private boolean matchesCause(EligibilityRuleRecord rule, AssessmentRequest req) {
        return rule.getCause() == null || rule.getCause() == req.cause();
    }

    private boolean matchesDelay(EligibilityRuleRecord rule, AssessmentRequest req) {
        if (req.delayMinutes() < rule.getDelayMinMinutes()) return false;
        return rule.getDelayMaxMinutes() == null || req.delayMinutes() <= rule.getDelayMaxMinutes();
    }

    private boolean matchesDistance(EligibilityRuleRecord rule, AssessmentRequest req) {
        if (req.flightDistanceKm() == null) return true;
        if (rule.getFlightDistanceKmMin() != null && req.flightDistanceKm() < rule.getFlightDistanceKmMin()) return false;
        if (rule.getFlightDistanceKmMax() != null && req.flightDistanceKm() > rule.getFlightDistanceKmMax()) return false;
        return true;
    }
}
