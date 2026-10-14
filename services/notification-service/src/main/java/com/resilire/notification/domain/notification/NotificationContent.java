package com.resilire.notification.domain.notification;

import java.util.Objects;

/** Rendered, channel-agnostic message content. Value object. */
public record NotificationContent(String title, String body) {

    public NotificationContent {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (body.isBlank()) {
            throw new IllegalArgumentException("body must not be blank");
        }
    }
}
