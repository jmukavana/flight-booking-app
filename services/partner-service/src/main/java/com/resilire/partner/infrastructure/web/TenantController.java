package com.resilire.partner.infrastructure.web;

import com.resilire.partner.application.TenantService;
import com.resilire.partner.infrastructure.persistence.TenantRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
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
@RequestMapping("/partners/tenants")
class TenantController {

    private final TenantService service;

    TenantController(TenantService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    List<TenantView> list() {
        return service.findAll().stream().map(TenantView::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    TenantView create(@Valid @RequestBody CreateRequest body) {
        return TenantView.from(service.createTenant(body.name(), body.email(), body.plan()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    TenantView get(@PathVariable String id) {
        return service.findById(id)
                .map(TenantView::from)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + id));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('admin')")
    TenantView suspend(@PathVariable String id) {
        return TenantView.from(service.suspendTenant(id));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('admin')")
    TenantView activate(@PathVariable String id) {
        return TenantView.from(service.activateTenant(id));
    }

    record CreateRequest(@NotBlank String name, @NotBlank @Email String email, @NotBlank String plan) {}

    record TenantView(String id, String name, String email, String status, String plan, Instant createdAt) {
        static TenantView from(TenantRecord r) {
            return new TenantView(r.getId(), r.getName(), r.getEmail(),
                    r.getStatus().name(), r.getPlan(), r.getCreatedAt());
        }
    }
}
