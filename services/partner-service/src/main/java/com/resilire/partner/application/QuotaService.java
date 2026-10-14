package com.resilire.partner.application;

import com.resilire.partner.infrastructure.persistence.TenantQuotaJpaRepository;
import com.resilire.partner.infrastructure.persistence.TenantQuotaRecord;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotaService {

    private final TenantQuotaJpaRepository quotas;

    public QuotaService(TenantQuotaJpaRepository quotas) {
        this.quotas = quotas;
    }

    @Transactional
    public TenantQuotaRecord getOrCreate(String tenantId, String plan) {
        return quotas.findByTenantId(tenantId).orElseGet(() -> {
            TenantQuotaRecord r = new TenantQuotaRecord();
            r.setId(UUID.randomUUID().toString());
            r.setTenantId(tenantId);
            r.setPlan(plan);
            applyPlanDefaults(r, plan);
            r.setCreatedAt(Instant.now());
            return quotas.save(r);
        });
    }

    @Transactional
    public TenantQuotaRecord updateQuota(String tenantId, String plan,
                                         int requestsPerMinute, int burstCapacity) {
        TenantQuotaRecord r = quotas.findByTenantId(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Quota not found for tenant: " + tenantId));
        r.setPlan(plan);
        r.setRequestsPerMinute(requestsPerMinute);
        r.setBurstCapacity(burstCapacity);
        r.setUpdatedAt(Instant.now());
        return quotas.save(r);
    }

    @Transactional(readOnly = true)
    public TenantQuotaRecord findByTenantId(String tenantId) {
        return quotas.findByTenantId(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Quota not found for tenant: " + tenantId));
    }

    private void applyPlanDefaults(TenantQuotaRecord r, String plan) {
        switch (plan) {
            case "STARTER" -> { r.setRequestsPerMinute(20); r.setBurstCapacity(30); }
            case "PROFESSIONAL" -> { r.setRequestsPerMinute(100); r.setBurstCapacity(150); }
            case "ENTERPRISE" -> { r.setRequestsPerMinute(500); r.setBurstCapacity(750); }
            default -> { r.setRequestsPerMinute(20); r.setBurstCapacity(30); }
        }
    }
}
