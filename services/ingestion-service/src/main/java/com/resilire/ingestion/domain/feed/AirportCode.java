package com.resilire.ingestion.domain.feed;

public record AirportCode(String value) {

    public AirportCode {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("airportCode must not be blank");
        value = value.toUpperCase();
        if (value.length() != 3) throw new IllegalArgumentException("IATA airport code must be 3 characters: " + value);
    }

    public static AirportCode of(String value) {
        return new AirportCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
