package com.resilire.notification.domain.notification;

import java.time.Instant;
import java.util.Objects;

/**
 * The recorded outcome of dispatching a notification — the unit of the delivery
 * log (published as {@code NotificationDelivered}). {@code channel} is the channel
 * that succeeded, or {@code null} when suppressed or failed. Value object.
 */
public record DeliveryResult(
        NotificationId notificationId,
        TravellerId traveller,
        NotificationType type,
        NotificationPriority priority,
        DeliveryStatus status,
        Channel channel,
        Instant at) {

    public DeliveryResult {
        Objects.requireNonNull(notificationId, "notificationId");
        Objects.requireNonNull(traveller, "traveller");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(at, "at");
    }

    public static DeliveryResult delivered(NotificationMessage message, Channel channel, Instant at) {
        return new DeliveryResult(message.id(), message.traveller(), message.type(),
                message.priority(), DeliveryStatus.DELIVERED, channel, at);
    }

    public static DeliveryResult suppressed(NotificationMessage message, Instant at) {
        return new DeliveryResult(message.id(), message.traveller(), message.type(),
                message.priority(), DeliveryStatus.SUPPRESSED, null, at);
    }

    public static DeliveryResult failed(NotificationMessage message, Instant at) {
        return new DeliveryResult(message.id(), message.traveller(), message.type(),
                message.priority(), DeliveryStatus.FAILED, null, at);
    }
}
