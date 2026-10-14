package com.resilire.partner.infrastructure.web;

import com.resilire.partner.application.ApiKeyService;
import com.resilire.partner.infrastructure.persistence.TenantApiKeyRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partners/tenants/{tenantId}/keys")
class ApiKeyController {

    private final ApiKeyService service;

    ApiKeyController(ApiKeyService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    List<KeyView> list(@PathVariable String tenantId) {
        return service.findByTenantId(tenantId).stream().map(KeyView::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    IssuedKeyView issue(@PathVariable String tenantId, @Valid @RequestBody IssueRequest body) {
        ApiKeyService.IssuedKey issued = service.issueKey(tenantId, body.description());
        return new IssuedKeyView(issued.keyId(), issued.rawKey(),
                "Store this key securely — it will not be shown again.");
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    void revoke(@PathVariable String tenantId, @PathVariable String keyId) {
        service.revokeKey(keyId);
    }

    record IssueRequest(@NotBlank String description) {}

    record IssuedKeyView(String keyId, String rawKey, String warning) {}

    record KeyView(String id, String tenantId, String status, String description,
                   Instant createdAt, Instant revokedAt) {
        static KeyView from(TenantApiKeyRecord r) {
            return new KeyView(r.getId(), r.getTenantId(), r.getStatus().name(),
                    r.getDescription(), r.getCreatedAt(), r.getRevokedAt());
        }
    }
}
