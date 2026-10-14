package com.resilire.notification.domain.notification;

import java.time.Instant;
import java.util.Objects;

/**
 * A notification to deliver to one Traveller: what happened, how urgent it is, and
 * the rendered content. Built by the factories below from the integration events
 * the service consumes. Value object.
 */
public record NotificationMessage(
        NotificationId id,
        TravellerId traveller,
        NotificationType type,
        NotificationPriority priority,
        NotificationContent content,
        Instant createdAt) {

    public NotificationMessage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(traveller, "traveller");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static NotificationMessage orderConfirmed(TravellerId traveller, String pnr,
                                                     String origin, String destination) {
        NotificationContent content = new NotificationContent(
                "Booking confirmed: " + origin + " → " + destination,
                "Your booking " + pnr + " (" + origin + " → " + destination + ") is confirmed.");
        return new NotificationMessage(NotificationId.newId(), traveller, NotificationType.ORDER_CONFIRMED,
                NotificationPriority.NORMAL, content, Instant.now());
    }

    public static NotificationMessage orderCancelled(TravellerId traveller, String orderReference) {
        NotificationContent content = new NotificationContent(
                "Booking cancelled",
                "Your booking " + orderReference + " has been cancelled and any payment refunded.");
        return new NotificationMessage(NotificationId.newId(), traveller, NotificationType.ORDER_CANCELLED,
                NotificationPriority.NORMAL, content, Instant.now());
    }
}
