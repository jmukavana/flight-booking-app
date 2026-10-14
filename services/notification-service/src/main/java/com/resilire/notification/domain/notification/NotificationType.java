package com.resilire.notification.domain.notification;

/**
 * What happened, in the notification's language. Phase 1 covers the booking
 * lifecycle; {@code DISRUPTION_ALERT} (and its URGENT priority) arrives with
 * Detection in Phase 2 (roadmap.md §4).
 */
public enum NotificationType {
    ORDER_CONFIRMED,
    ORDER_CANCELLED
}
