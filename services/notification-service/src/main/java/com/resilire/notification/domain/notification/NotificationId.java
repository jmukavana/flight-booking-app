package com.resilire.notification.domain.notification;

import java.util.Objects;
import java.util.UUID;

/** Identity of a single notification. Value object. */
public record NotificationId(UUID value) {

    public NotificationId {
        Objects.requireNonNull(value, "notificationId value");
    }

    public static NotificationId newId() {
        return new NotificationId(UUID.randomUUID());
    }
}
