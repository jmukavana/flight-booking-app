package com.resilire.rightsclaims.infrastructure.web;

import com.resilire.rightsclaims.application.RulesetAdminService;
import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.EntitlementType;
import com.resilire.rightsclaims.domain.Jurisdiction;
import com.resilire.rightsclaims.infrastructure.persistence.EligibilityRuleRecord;
import com.resilire.rightsclaims.infrastructure.persistence.RulesetRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/rulesets")
@PreAuthorize("hasRole('admin')")
class RulesetAdminController {

    private final RulesetAdminService service;

    RulesetAdminController(RulesetAdminService service) {
        this.service = service;
    }

    @GetMapping
    List<RulesetView> list(@RequestParam @NotNull Jurisdiction jurisdiction) {
        return service.findAll(jurisdiction).stream().map(RulesetView::from).toList();
    }

    @GetMapping("/{id}")
    RulesetDetailView get(@PathVariable String id) {
        RulesetRecord rs = service.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ruleset not found: " + id));
        return RulesetDetailView.from(rs, service.findRules(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RulesetView create(@Valid @RequestBody CreateRulesetRequest body) {
        return RulesetView.from(service.createDraft(body.jurisdiction(), body.version(), body.effectiveFrom()));
    }

    @PostMapping("/{id}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    RuleView addRule(@PathVariable String id, @Valid @RequestBody AddRuleRequest body) {
        return RuleView.from(service.addRule(id, body.entitlementType(), body.cause(),
                body.delayMinMinutes(), body.delayMaxMinutes(),
                body.distanceKmMin(), body.distanceKmMax(),
                body.compensationAmount(), body.currency(), body.description()));
    }

    @PostMapping("/{id}/activate")
    RulesetView activate(@PathVariable String id) {
        return RulesetView.from(service.activate(id));
    }

    @PostMapping("/{id}/supersede")
    RulesetView supersede(@PathVariable String id) {
        return RulesetView.from(service.supersede(id));
    }

    record CreateRulesetRequest(@NotNull Jurisdiction jurisdiction,
                                @NotBlank String version,
                                @NotNull LocalDate effectiveFrom) {}

    record AddRuleRequest(@NotNull EntitlementType entitlementType,
                          @NotNull DisruptionCause cause,
                          int delayMinMinutes, Integer delayMaxMinutes,
                          Integer distanceKmMin, Integer distanceKmMax,
                          BigDecimal compensationAmount, String currency,
                          @NotBlank String description) {}

    record RulesetView(String id, String jurisdiction, String version,
                       String status, LocalDate effectiveFrom, Instant createdAt) {
        static RulesetView from(RulesetRecord r) {
            return new RulesetView(r.getId(), r.getJurisdiction().name(), r.getVersion(),
                    r.getStatus().name(), r.getEffectiveFrom(), r.getCreatedAt());
        }
    }

    record RuleView(String id, String entitlementType, String cause,
                    int delayMinMinutes, Integer delayMaxMinutes,
                    Integer distanceKmMin, Integer distanceKmMax,
                    BigDecimal compensationAmount, String currency, String description) {
        static RuleView from(EligibilityRuleRecord r) {
            return new RuleView(r.getId(),
                    r.getEntitlementType() != null ? r.getEntitlementType().name() : null,
                    r.getCause() != null ? r.getCause().name() : null,
                    r.getDelayMinMinutes(), r.getDelayMaxMinutes(),
                    r.getFlightDistanceKmMin(), r.getFlightDistanceKmMax(),
                    r.getCompensationAmount(), r.getCurrency(), r.getDescription());
        }
    }

    record RulesetDetailView(RulesetView ruleset, List<RuleView> rules) {
        static RulesetDetailView from(RulesetRecord rs, List<EligibilityRuleRecord> ruleList) {
            return new RulesetDetailView(RulesetView.from(rs),
                    ruleList.stream().map(RuleView::from).toList());
        }
    }
}
