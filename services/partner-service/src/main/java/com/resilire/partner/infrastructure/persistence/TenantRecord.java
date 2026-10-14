package com.resilire.partner.infrastructure.persistence;

import com.resilire.partner.domain.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class TenantRecord {

    @Id private String id;
    @Column(nullable = false, unique = true) private String name;
    @Column(nullable = false) private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private TenantStatus status;
    @Column(nullable = false) private String plan;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
