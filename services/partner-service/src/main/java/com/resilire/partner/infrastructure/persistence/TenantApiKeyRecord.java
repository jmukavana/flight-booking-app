package com.resilire.partner.infrastructure.persistence;

import com.resilire.partner.domain.ApiKeyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenant_api_keys",
       indexes = @Index(name = "idx_api_keys_tenant_id", columnList = "tenant_id"))
@Getter
@Setter
@NoArgsConstructor
public class TenantApiKeyRecord {

    @Id private String id;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Column(name = "key_hash", nullable = false, unique = true) private String keyHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private ApiKeyStatus status;
    @Column private String description;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "revoked_at") private Instant revokedAt;
}
