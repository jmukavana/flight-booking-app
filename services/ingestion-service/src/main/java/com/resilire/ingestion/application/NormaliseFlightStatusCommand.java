package com.resilire.ingestion.application;

import java.util.List;

public record NormaliseFlightStatusCommand(
        String flightNumber,
        String origin,
        String destination,
        String status,
        String scheduledDeparture,
        String estimatedDeparture,
        String source) {
}
