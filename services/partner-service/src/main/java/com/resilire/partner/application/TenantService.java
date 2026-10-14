package com.resilire.partner.application;

import com.resilire.partner.domain.TenantStatus;
import com.resilire.partner.infrastructure.persistence.TenantJpaRepository;
import com.resilire.partner.infrastructure.persistence.TenantRecord;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TenantService {

    private final TenantJpaRepository tenants;

    public TenantService(TenantJpaRepository tenants) {
        this.tenants = tenants;
    }

    public TenantRecord createTenant(String name, String email, String plan) {
        if (tenants.existsByName(name)) {
            throw new IllegalArgumentException("Tenant already exists: " + name);
        }
        TenantRecord t = new TenantRecord();
        t.setId(UUID.randomUUID().toString());
        t.setName(name);
        t.setEmail(email);
        t.setStatus(TenantStatus.ACTIVE);
        t.setPlan(plan);
        t.setCreatedAt(Instant.now());
        return tenants.save(t);
    }

    public Optional<TenantRecord> findById(String id) {
        return tenants.findById(id);
    }

    public List<TenantRecord> findAll() {
        return tenants.findAll();
    }

    public TenantRecord suspendTenant(String id) {
        TenantRecord t = tenants.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + id));
        t.setStatus(TenantStatus.SUSPENDED);
        return tenants.save(t);
    }

    public TenantRecord activateTenant(String id) {
        TenantRecord t = tenants.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + id));
        t.setStatus(TenantStatus.ACTIVE);
        return tenants.save(t);
    }
}
