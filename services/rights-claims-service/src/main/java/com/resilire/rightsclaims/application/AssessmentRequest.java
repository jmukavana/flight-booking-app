package com.resilire.rightsclaims.application;

import com.resilire.rightsclaims.domain.DisruptionCause;
import com.resilire.rightsclaims.domain.Jurisdiction;

public record AssessmentRequest(
        String disruptionId,
        String flightId,
        String origin,
        String destination,
        Jurisdiction jurisdiction,
        int delayMinutes,
        DisruptionCause cause,
        Integer flightDistanceKm) {
}
