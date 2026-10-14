package com.resilire.disruptiondetection.domain.disruption;

/** Severity bands used downstream by notification and recovery services. */
public enum DisruptionSeverity {
    /** Delay 45–119 minutes: early warning, no EU261 rights yet. */
    MODERATE,
    /** Delay ≥ 120 minutes or cancellation: EU261 rights likely triggered. */
    SEVERE,
    /** Flight cancelled outright. */
    CANCELLED
}
