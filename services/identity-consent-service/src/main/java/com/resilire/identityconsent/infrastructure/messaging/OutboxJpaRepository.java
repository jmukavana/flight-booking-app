package com.resilire.identityconsent.infrastructure.messaging;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxJpaRepository extends JpaRepository<OutboxRecord, UUID> {

    List<OutboxRecord> findByPublishedAtIsNullOrderByOccurredAtAsc(Limit limit);
}
