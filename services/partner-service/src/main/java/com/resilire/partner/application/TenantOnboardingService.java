package com.resilire.partner.application;

import com.resilire.partner.infrastructure.persistence.TenantQuotaRecord;
import com.resilire.partner.infrastructure.persistence.TenantRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the complete B2B partner onboarding saga in one atomic unit:
 * create tenant → issue API key → provision quota plan. The raw API key is
 * returned once and must be forwarded to the partner — it is never retrievable
 * after this call (stored as SHA-256 hash only, driver 5 "trust in the platform").
 */
@Service
public class TenantOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(TenantOnboardingService.class);

    private final TenantService tenants;
    private final ApiKeyService apiKeys;
    private final QuotaService quotas;

    public TenantOnboardingService(TenantService tenants, ApiKeyService apiKeys,
                                   QuotaService quotas) {
        this.tenants = tenants;
        this.apiKeys = apiKeys;
        this.quotas = quotas;
    }

    @Transactional
    public OnboardingResult onboard(String name, String email, String plan) {
        TenantRecord tenant = tenants.createTenant(name, email, plan);
        ApiKeyService.IssuedKey key = apiKeys.issueKey(tenant.getId(), "default");
        TenantQuotaRecord quota = quotas.getOrCreate(tenant.getId(), plan);

        log.info("Onboarded tenant {} (plan={}, rpm={})", tenant.getId(), plan,
                quota.getRequestsPerMinute());

        return new OnboardingResult(tenant.getId(), name, key.rawKey(), plan,
                quota.getRequestsPerMinute(), quota.getBurstCapacity());
    }

    public record OnboardingResult(String tenantId, String name, String rawApiKey, String plan,
                                   int requestsPerMinute, int burstCapacity) {}
}
