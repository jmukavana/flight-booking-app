package com.resilire.partner.infrastructure.web;

import com.resilire.partner.application.QuotaService;
import com.resilire.partner.infrastructure.persistence.TenantQuotaRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partners/tenants/{tenantId}/quota")
@PreAuthorize("hasRole('admin')")
class QuotaController {

    private final QuotaService service;

    QuotaController(QuotaService service) {
        this.service = service;
    }

    @GetMapping
    QuotaView get(@PathVariable String tenantId) {
        return QuotaView.from(service.findByTenantId(tenantId));
    }

    @PutMapping
    QuotaView update(@PathVariable String tenantId,
                     @Valid @RequestBody UpdateQuotaRequest body) {
        return QuotaView.from(service.updateQuota(tenantId,
                body.plan(), body.requestsPerMinute(), body.burstCapacity()));
    }

    record UpdateQuotaRequest(@NotBlank String plan,
                              @Positive int requestsPerMinute,
                              @Positive int burstCapacity) {}

    record QuotaView(String id, String tenantId, String plan,
                     int requestsPerMinute, int burstCapacity,
                     Instant createdAt, Instant updatedAt) {
        static QuotaView from(TenantQuotaRecord r) {
            return new QuotaView(r.getId(), r.getTenantId(), r.getPlan(),
                    r.getRequestsPerMinute(), r.getBurstCapacity(),
                    r.getCreatedAt(), r.getUpdatedAt());
        }
    }
}
