package com.resilire.recoveryorchestrator.infrastructure.messaging;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface OutboxJpaRepository extends JpaRepository<OutboxRecord, String> {
    List<OutboxRecord> findTop50ByPublishedAtIsNullOrderByOccurredAtAsc();
}
