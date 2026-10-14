package com.resilire.notification.domain.notification;

/** Outcome of attempting to deliver a notification. */
public enum DeliveryStatus {
    DELIVERED,
    SUPPRESSED,
    FAILED
}
