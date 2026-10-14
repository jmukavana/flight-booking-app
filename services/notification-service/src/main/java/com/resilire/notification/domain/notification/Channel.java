package com.resilire.notification.domain.notification;

/** A delivery channel. The fallback chain tries channels in the Traveller's preferred order. */
public enum Channel {
    EMAIL,
    SMS,
    PUSH
}
