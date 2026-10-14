package com.resilire.notification.domain.notification;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * A daily do-not-disturb window in the Traveller's zone. Handles windows that wrap
 * past midnight (e.g. 22:00–07:00). NORMAL notifications are suppressed while quiet;
 * URGENT ones ignore it (ADR-14). Value object.
 */
public record QuietHours(boolean enabled, LocalTime start, LocalTime end, ZoneId zone) {

    public QuietHours {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(zone, "zone");
    }

    public static QuietHours none() {
        return new QuietHours(false, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, ZoneOffset.UTC);
    }

    public static QuietHours of(LocalTime start, LocalTime end, ZoneId zone) {
        return new QuietHours(true, start, end, zone);
    }

    public boolean isQuietAt(Instant instant) {
        if (!enabled || start.equals(end)) {
            return false;
        }
        LocalTime now = instant.atZone(zone).toLocalTime();
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        // Window wraps past midnight.
        return !now.isBefore(start) || now.isBefore(end);
    }
}
