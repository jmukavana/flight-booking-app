package com.resilire.rightsclaims.infrastructure.web;

import com.resilire.rightsclaims.application.ClaimApplicationService;
import com.resilire.rightsclaims.infrastructure.persistence.ClaimRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/claims")
class ClaimsController {

    private final ClaimApplicationService service;

    ClaimsController(ClaimApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClaimView submit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SubmitRequest body) {
        return ClaimView.from(service.submit(body.assessmentId(), jwt.getSubject()));
    }

    @GetMapping("/{id}")
    ClaimView getClaim(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        ClaimRecord claim = service.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Claim not found: " + id));
        if (!claim.getTravellerId().equals(jwt.getSubject())) {
            throw new NoSuchElementException("Claim not found: " + id);
        }
        return ClaimView.from(claim);
    }

    @GetMapping
    List<ClaimView> listClaims(@AuthenticationPrincipal Jwt jwt) {
        return service.findByTraveller(jwt.getSubject()).stream()
                .map(ClaimView::from)
                .toList();
    }

    record SubmitRequest(@NotBlank String assessmentId) {}

    record ClaimView(String claimId, String assessmentId, String status,
                     Instant submittedAt, Instant resolvedAt, String paymentRef) {
        static ClaimView from(ClaimRecord r) {
            return new ClaimView(r.getId(), r.getAssessmentId(), r.getStatus().name(),
                    r.getSubmittedAt(), r.getResolvedAt(), r.getPaymentRef());
        }
    }
}
