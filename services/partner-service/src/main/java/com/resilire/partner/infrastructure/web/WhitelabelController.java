package com.resilire.partner.infrastructure.web;

import com.resilire.partner.application.WhitelabelService;
import com.resilire.partner.infrastructure.persistence.WhitelabelConfigRecord;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partners/tenants/{tenantId}/whitelabel")
@PreAuthorize("hasRole('admin')")
class WhitelabelController {

    private final WhitelabelService service;

    WhitelabelController(WhitelabelService service) {
        this.service = service;
    }

    @GetMapping
    WhitelabelView get(@PathVariable String tenantId) {
        return service.findByTenantId(tenantId)
                .map(WhitelabelView::from)
                .orElseThrow(() -> new NoSuchElementException("Whitelabel config not found for tenant: " + tenantId));
    }

    @PutMapping
    WhitelabelView upsert(@PathVariable String tenantId,
                          @Valid @RequestBody UpsertWhitelabelRequest body) {
        return WhitelabelView.from(service.upsertConfig(tenantId,
                body.logoUrl(), body.primaryColor(), body.customDomain()));
    }

    record UpsertWhitelabelRequest(String logoUrl, String primaryColor, String customDomain) {}

    record WhitelabelView(String id, String tenantId, String logoUrl,
                          String primaryColor, String customDomain,
                          Instant createdAt, Instant updatedAt) {
        static WhitelabelView from(WhitelabelConfigRecord r) {
            return new WhitelabelView(r.getId(), r.getTenantId(), r.getLogoUrl(),
                    r.getPrimaryColor(), r.getCustomDomain(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }
}
