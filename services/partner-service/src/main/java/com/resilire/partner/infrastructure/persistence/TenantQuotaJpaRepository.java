package com.resilire.partner.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantQuotaJpaRepository extends JpaRepository<TenantQuotaRecord, String> {
    Optional<TenantQuotaRecord> findByTenantId(String tenantId);
}
