package com.resilire.recoveryorchestrator.infrastructure.web;

import com.resilire.recoveryorchestrator.application.RecoveryOrchestratorService;
import com.resilire.recoveryorchestrator.domain.RecoveryCaseStatus;
import com.resilire.recoveryorchestrator.domain.RecoveryOptionType;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryCaseRecord;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryOptionRecord;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recovery/cases")
class RecoveryController {

    private final RecoveryOrchestratorService service;

    RecoveryController(RecoveryOrchestratorService service) {
        this.service = service;
    }

    @GetMapping
    List<CaseView> list(@RequestParam(defaultValue = "OPEN") String status) {
        return service.findByStatus(RecoveryCaseStatus.valueOf(status)).stream()
                .map(c -> CaseView.from(c, service.findOptions(c.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    CaseView getCase(@PathVariable String id) {
        RecoveryCaseRecord c = service.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recovery case not found: " + id));
        return CaseView.from(c, service.findOptions(id));
    }

    @PostMapping("/{id}/resolve")
    CaseView resolve(@PathVariable String id) {
        return CaseView.from(service.resolveCase(id), service.findOptions(id));
    }

    @PostMapping("/{id}/fail")
    CaseView fail(@PathVariable String id) {
        return CaseView.from(service.failCase(id), service.findOptions(id));
    }

    record OptionView(String optionType, String description) {
        static OptionView from(RecoveryOptionRecord r) {
            return new OptionView(r.getOptionType().name(), r.getDescription());
        }
    }

    record CaseView(String caseId, String disruptionId, String flightId, String origin,
                    String destination, String disruptionCause, String severity,
                    int delayMinutes, String status, boolean autoApproved,
                    Instant openedAt, Instant resolvedAt, List<OptionView> options) {
        static CaseView from(RecoveryCaseRecord c, List<RecoveryOptionRecord> opts) {
            return new CaseView(c.getId(), c.getDisruptionId(), c.getFlightId(),
                    c.getOrigin(), c.getDestination(), c.getDisruptionCause(),
                    c.getSeverity(), c.getDelayMinutes(), c.getStatus().name(),
                    c.isAutoApproved(), c.getOpenedAt(), c.getResolvedAt(),
                    opts.stream().map(OptionView::from).toList());
        }
    }
}
