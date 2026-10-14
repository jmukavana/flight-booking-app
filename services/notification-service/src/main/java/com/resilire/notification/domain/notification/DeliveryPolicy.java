package com.resilire.notification.domain.notification;

import java.time.Instant;

/**
 * The quiet-hours rule (FR-11). URGENT notifications always go through (the
 * reserved priority lane, ADR-14); NORMAL ones are held back while the Traveller
 * is in quiet hours.
 */
public final class DeliveryPolicy {

    private DeliveryPolicy() {
    }

    public static boolean shouldDeliver(NotificationPreferences preferences,
                                        NotificationPriority priority, Instant at) {
        return priority == NotificationPriority.URGENT || !preferences.quietHours().isQuietAt(at);
    }
}
