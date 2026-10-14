package com.resilire.partner.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhitelabelConfigJpaRepository extends JpaRepository<WhitelabelConfigRecord, String> {
    Optional<WhitelabelConfigRecord> findByTenantId(String tenantId);
}
