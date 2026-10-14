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
@Table(name = "whitelabel_configs")
@Getter
@Setter
@NoArgsConstructor
public class WhitelabelConfigRecord {

    @Id private String id;
    @Column(name = "tenant_id", nullable = false, unique = true) private String tenantId;
    @Column(name = "logo_url") private String logoUrl;
    @Column(name = "primary_color") private String primaryColor;
    @Column(name = "custom_domain") private String customDomain;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
