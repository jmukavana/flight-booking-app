package com.resilire.notification.domain.notification;

import java.util.List;
import java.util.Objects;

/**
 * A Traveller's delivery preferences: the channel fallback order and quiet hours.
 * Phase 1 serves a default profile for everyone — per-Traveller management is FR-8,
 * delivered in Phase 2 (roadmap.md §4). Value object.
 */
public record NotificationPreferences(List<Channel> channelOrder, QuietHours quietHours) {

    public NotificationPreferences {
        Objects.requireNonNull(channelOrder, "channelOrder");
        Objects.requireNonNull(quietHours, "quietHours");
        if (channelOrder.isEmpty()) {
            throw new IllegalArgumentException("channelOrder must list at least one channel");
        }
        channelOrder = List.copyOf(channelOrder);
    }

    public static NotificationPreferences defaults() {
        return new NotificationPreferences(List.of(Channel.EMAIL, Channel.PUSH, Channel.SMS), QuietHours.none());
    }
}
