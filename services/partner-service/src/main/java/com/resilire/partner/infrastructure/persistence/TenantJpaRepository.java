package com.resilire.partner.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<TenantRecord, String> {
    Optional<TenantRecord> findByName(String name);
    boolean existsByName(String name);
}
