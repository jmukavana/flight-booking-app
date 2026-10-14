package com.resilire.disruptiondetection.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "disruptions", indexes = {
        @Index(name = "idx_disruption_flight_status", columnList = "flight_id, status")
})
@Getter
@Setter
@NoArgsConstructor
public class DisruptionRecord {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "flight_id", nullable = false)
    private String flightId;

    @Column(name = "origin", nullable = false)
    private String origin;

    @Column(name = "destination", nullable = false)
    private String destination;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "cause", nullable = false)
    private String cause;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "delay_minutes", nullable = false)
    private int delayMinutes;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;
}
