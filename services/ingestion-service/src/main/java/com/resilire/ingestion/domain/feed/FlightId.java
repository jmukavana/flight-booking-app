package com.resilire.ingestion.domain.feed;

public record FlightId(String value) {

    public FlightId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("flightId must not be blank");
        value = value.toUpperCase();
    }

    public static FlightId of(String value) {
        return new FlightId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
