package com.resilire.notification.application;

import com.resilire.notification.domain.notification.Channel;
import com.resilire.notification.domain.notification.ChannelDeliveryException;
import com.resilire.notification.domain.notification.NotificationContent;
import com.resilire.notification.domain.notification.TravellerId;

/**
 * Outbound port for one delivery channel (the Comms multi-channel adapter,
 * architecture.md §10). Each implementation owns a {@link Channel}; the dispatcher
 * tries them in the Traveller's preferred order.
 */
public interface NotificationChannel {

    Channel channel();

    /**
     * @throws ChannelDeliveryException if the channel cannot deliver — the
     *         dispatcher then falls back to the next channel.
     */
    void send(TravellerId recipient, NotificationContent content);
}
