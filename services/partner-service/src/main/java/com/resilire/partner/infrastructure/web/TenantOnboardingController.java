package com.resilire.partner.infrastructure.web;

import com.resilire.partner.application.TenantOnboardingService;
import com.resilire.partner.application.TenantOnboardingService.OnboardingResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partners/onboard")
@PreAuthorize("hasRole('admin')")
class TenantOnboardingController {

    private final TenantOnboardingService service;

    TenantOnboardingController(TenantOnboardingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OnboardingResult onboard(@Valid @RequestBody OnboardRequest body) {
        return service.onboard(body.name(), body.email(), body.plan());
    }

    record OnboardRequest(@NotBlank String name,
                          @NotBlank @Email String email,
                          @NotBlank String plan) {}
}
