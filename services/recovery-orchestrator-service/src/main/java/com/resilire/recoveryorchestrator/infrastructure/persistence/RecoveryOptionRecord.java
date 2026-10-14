package com.resilire.recoveryorchestrator.infrastructure.persistence;

import com.resilire.recoveryorchestrator.domain.RecoveryOptionType;
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
@Table(name = "recovery_options")
@Getter
@Setter
@NoArgsConstructor
public class RecoveryOptionRecord {

    @Id private String id;
    @Column(name = "case_id", nullable = false) private String caseId;
    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false) private RecoveryOptionType optionType;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
