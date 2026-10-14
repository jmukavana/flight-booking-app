package com.resilire.notification.infrastructure.channel;

import com.resilire.notification.application.NotificationChannel;
import com.resilire.notification.domain.notification.Channel;
import com.resilire.notification.domain.notification.NotificationContent;
import com.resilire.notification.domain.notification.TravellerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Push channel — Phase 1 stand-in for the FCM/APNs adapter (logs instead of sending). */
@Component
class PushChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(PushChannel.class);

    @Override
    public Channel channel() {
        return Channel.PUSH;
    }

    @Override
    public void send(TravellerId recipient, NotificationContent content) {
        log.info("PUSH to {}: {}", recipient.value(), content.title());
    }
}
