package com.resilire.notification.domain.notification;

/**
 * Delivery priority. {@code URGENT} is the reserved priority lane for disruption
 * alerts (ADR-14) — it bypasses quiet hours and load shedding to meet the ≤2-min
 * SLA. {@code NORMAL} (e.g. booking confirmations) respects quiet hours.
 */
public enum NotificationPriority {
    NORMAL,
    URGENT
}
