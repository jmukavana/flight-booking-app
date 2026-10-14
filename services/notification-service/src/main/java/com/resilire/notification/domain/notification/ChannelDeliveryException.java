package com.resilire.notification.domain.notification;

/** Thrown by a channel adapter when delivery fails, triggering the fallback chain. */
public class ChannelDeliveryException extends RuntimeException {

    public ChannelDeliveryException(Channel channel, String reason) {
        super("Delivery via " + channel + " failed: " + reason);
    }
}
