package com.resilire.partner.infrastructure.persistence;

import com.resilire.partner.domain.ReferralStatus;
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
@Table(name = "referrals")
@Getter
@Setter
@NoArgsConstructor
public class ReferralRecord {

    @Id private String id;
    @Column(name = "referrer_id", nullable = false) private String referrerId;
    @Column(name = "referred_id", nullable = false) private String referredId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private ReferralStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "credited_at") private Instant creditedAt;
}
