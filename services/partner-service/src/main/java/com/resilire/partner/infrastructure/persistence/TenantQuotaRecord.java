package com.resilire.partner.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenant_quotas")
@Getter
@Setter
@NoArgsConstructor
public class TenantQuotaRecord {

    @Id private String id;
    @Column(name = "tenant_id", nullable = false, unique = true) private String tenantId;
    @Column(name = "requests_per_minute", nullable = false) private int requestsPerMinute;
    @Column(name = "burst_capacity", nullable = false) private int burstCapacity;
    @Column(nullable = false) private String plan;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
