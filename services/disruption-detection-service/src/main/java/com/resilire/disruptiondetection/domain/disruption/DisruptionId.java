package com.resilire.disruptiondetection.domain.disruption;

import java.util.UUID;

public record DisruptionId(String value) {

    public DisruptionId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("disruptionId must not be blank");
    }

    public static DisruptionId newId() {
        return new DisruptionId(UUID.randomUUID().toString());
    }

    public static DisruptionId of(String value) {
        return new DisruptionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
