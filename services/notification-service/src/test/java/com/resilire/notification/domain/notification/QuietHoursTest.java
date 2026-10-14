package com.resilire.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Quiet-hours windows, including the wrap-past-midnight case. */
class QuietHoursTest {

    @Test
    void none_is_never_quiet() {
        assertThat(QuietHours.none().isQuietAt(Instant.parse("2026-07-01T03:00:00Z"))).isFalse();
    }

    @Test
    void same_day_window() {
        QuietHours window = QuietHours.of(LocalTime.of(9, 0), LocalTime.of(17, 0), ZoneOffset.UTC);

        assertThat(window.isQuietAt(Instant.parse("2026-07-01T12:00:00Z"))).isTrue();
        assertThat(window.isQuietAt(Instant.parse("2026-07-01T08:59:00Z"))).isFalse();
        assertThat(window.isQuietAt(Instant.parse("2026-07-01T17:00:00Z"))).isFalse(); // end exclusive
    }

    @Test
    void window_wrapping_past_midnight() {
        QuietHours window = QuietHours.of(LocalTime.of(22, 0), LocalTime.of(7, 0), ZoneOffset.UTC);

        assertThat(window.isQuietAt(Instant.parse("2026-07-01T23:30:00Z"))).isTrue();
        assertThat(window.isQuietAt(Instant.parse("2026-07-01T05:00:00Z"))).isTrue();
        assertThat(window.isQuietAt(Instant.parse("2026-07-01T12:00:00Z"))).isFalse();
    }

    @Test
    void quiet_hours_honour_the_traveller_zone() {
        // 23:00 in UTC+2 is 21:00 UTC — outside a 22:00–07:00 window evaluated in the zone.
        QuietHours window = QuietHours.of(LocalTime.of(22, 0), LocalTime.of(7, 0), ZoneOffset.ofHours(2));

        assertThat(window.isQuietAt(Instant.parse("2026-07-01T21:00:00Z"))).isTrue();   // 23:00 local
        assertThat(window.isQuietAt(Instant.parse("2026-07-01T19:00:00Z"))).isFalse();  // 21:00 local
    }
}
