package com.resilire.rightsclaims.infrastructure.web;

import com.resilire.rightsclaims.application.AssessmentRequest;
import com.resilire.rightsclaims.application.EligibilityService;
import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.Jurisdiction;
import com.resilire.rightsclaims.infrastructure.persistence.AssessmentRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eligibility")
class EligibilityController {

    private final EligibilityService service;

    EligibilityController(EligibilityService service) {
        this.service = service;
    }

    @PostMapping("/assess")
    AssessmentView assess(@Valid @RequestBody AssessRequest body) {
        AssessmentRecord record = service.assess(new AssessmentRequest(
                body.disruptionId(), body.flightId(), body.origin(), body.destination(),
                body.jurisdiction(), body.delayMinutes(), body.cause(), body.flightDistanceKm()));
        return AssessmentView.from(record);
    }

    @GetMapping("/assessments/{id}")
    ResponseEntity<AssessmentView> getAssessment(@PathVariable String id) {
        return service.findById(id)
                .map(r -> ResponseEntity.ok(AssessmentView.from(r)))
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + id));
    }

    record AssessRequest(
            String disruptionId,
            @NotBlank String flightId,
            @NotBlank String origin,
            @NotBlank String destination,
            @NotNull Jurisdiction jurisdiction,
            int delayMinutes,
            @NotNull DisruptionCause cause,
            Integer flightDistanceKm) {}

    record AssessmentView(String assessmentId, String flightId, String jurisdiction,
                          boolean eligible, String entitlementType,
                          BigDecimal compensationAmount, String currency,
                          String reasoning, String rulesetVersion, Instant assessedAt) {
        static AssessmentView from(AssessmentRecord r) {
            return new AssessmentView(r.getId(), r.getFlightId(),
                    r.getJurisdiction().name(), r.isEligible(),
                    r.getEntitlementType() != null ? r.getEntitlementType().name() : null,
                    r.getCompensationAmount(), r.getCurrency(),
                    r.getReasoning(), r.getRulesetVersion(), r.getAssessedAt());
        }
    }
}
