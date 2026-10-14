package com.resilire.notification.application;

import com.resilire.notification.domain.notification.DeliveryResult;

/**
 * Outbound port for the {@code NotificationDelivered} integration event — the
 * append-only delivery log (architecture.md §6). Delivery is at-least-once;
 * consumers dedupe on the notification id.
 */
public interface DeliveryEventPublisher {

    void publish(DeliveryResult result);
}
