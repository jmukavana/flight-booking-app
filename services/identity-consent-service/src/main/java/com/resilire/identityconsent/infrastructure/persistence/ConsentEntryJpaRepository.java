package com.resilire.identityconsent.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentEntryJpaRepository extends JpaRepository<ConsentEntryRecord, UUID> {

    List<ConsentEntryRecord> findByUserIdOrderByVersionAsc(String userId);
}
