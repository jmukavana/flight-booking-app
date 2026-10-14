package com.resilire.notification.infrastructure.messaging;

import com.resilire.notification.domain.notification.Channel;
import com.resilire.notification.domain.notification.DeliveryResult;
import java.util.UUID;

/**
 * Published language for the {@code NotificationDelivered} integration event — the
 * delivery log (architecture.md §6). {@code channel} is null when the notification
 * was suppressed (quiet hours) or failed on every channel.
 */
public record NotificationDeliveredPayload(
        String eventId,
        String eventType,
        String notificationId,
        String travellerId,
        String type,
        String priority,
        String status,
        String channel,
        String at) {

    public static final String EVENT_TYPE = "NotificationDelivered";
    public static final String TOPIC = "notification.delivered";

    public static NotificationDeliveredPayload from(DeliveryResult result) {
        Channel channel = result.channel();
        return new NotificationDeliveredPayload(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                result.notificationId().value().toString(),
                result.traveller().value(),
                result.type().name(),
                result.priority().name(),
                result.status().name(),
                channel == null ? null : channel.name(),
                result.at().toString());
    }
}
