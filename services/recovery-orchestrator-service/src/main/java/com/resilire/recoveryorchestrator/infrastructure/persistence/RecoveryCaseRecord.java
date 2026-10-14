package com.resilire.recoveryorchestrator.infrastructure.persistence;

import com.resilire.recoveryorchestrator.domain.RecoveryCaseStatus;
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
@Table(name = "recovery_cases")
@Getter
@Setter
@NoArgsConstructor
public class RecoveryCaseRecord {

    @Id private String id;
    @Column(name = "disruption_id", nullable = false, unique = true) private String disruptionId;
    @Column(name = "flight_id", nullable = false) private String flightId;
    @Column(nullable = false) private String origin;
    @Column(nullable = false) private String destination;
    @Column(name = "disruption_cause", nullable = false) private String disruptionCause;
    @Column(nullable = false) private String severity;
    @Column(name = "delay_minutes") private int delayMinutes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private RecoveryCaseStatus status;
    @Column(name = "auto_approved") private boolean autoApproved;
    @Column(name = "opened_at", nullable = false) private Instant openedAt;
    @Column(name = "resolved_at") private Instant resolvedAt;
}
