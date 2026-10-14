package com.resilire.partner.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantApiKeyJpaRepository extends JpaRepository<TenantApiKeyRecord, String> {
    List<TenantApiKeyRecord> findByTenantId(String tenantId);
    Optional<TenantApiKeyRecord> findByKeyHash(String keyHash);
}
