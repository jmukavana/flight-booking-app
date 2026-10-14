package com.resilire.rightsclaims.infrastructure.messaging;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

interface OutboxJpaRepository extends JpaRepository<OutboxRecord, String> {
    List<OutboxRecord> findByPublishedAtIsNullOrderByOccurredAtAsc(Limit limit);
}
