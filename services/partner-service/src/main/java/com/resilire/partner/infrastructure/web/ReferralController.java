package com.resilire.partner.infrastructure.web;

import com.resilire.partner.application.ReferralService;
import com.resilire.partner.domain.ReferralStatus;
import com.resilire.partner.infrastructure.persistence.ReferralRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partners/referrals")
@PreAuthorize("hasRole('admin')")
class ReferralController {

    private final ReferralService service;

    ReferralController(ReferralService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReferralView create(@Valid @RequestBody CreateReferralRequest body) {
        return ReferralView.from(service.createReferral(body.referrerId(), body.referredId()));
    }

    @PostMapping("/{id}/credit")
    ReferralView credit(@PathVariable String id) {
        return ReferralView.from(service.creditReferral(id));
    }

    @GetMapping("/by-referrer/{referrerId}")
    List<ReferralView> listByReferrer(@PathVariable String referrerId) {
        return service.findByReferrer(referrerId).stream().map(ReferralView::from).toList();
    }

    record CreateReferralRequest(@NotBlank String referrerId, @NotBlank String referredId) {}

    record ReferralView(String id, String referrerId, String referredId,
                        ReferralStatus status, Instant createdAt, Instant creditedAt) {
        static ReferralView from(ReferralRecord r) {
            return new ReferralView(r.getId(), r.getReferrerId(), r.getReferredId(),
                    r.getStatus(), r.getCreatedAt(), r.getCreditedAt());
        }
    }
}
