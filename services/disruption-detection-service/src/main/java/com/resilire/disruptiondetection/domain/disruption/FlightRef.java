package com.resilire.disruptiondetection.domain.disruption;

public record FlightRef(String flightId, String origin, String destination) {

    public FlightRef {
        if (flightId == null || flightId.isBlank()) throw new IllegalArgumentException("flightId must not be blank");
        if (origin == null || origin.isBlank()) throw new IllegalArgumentException("origin must not be blank");
        if (destination == null || destination.isBlank()) throw new IllegalArgumentException("destination must not be blank");
    }
}
