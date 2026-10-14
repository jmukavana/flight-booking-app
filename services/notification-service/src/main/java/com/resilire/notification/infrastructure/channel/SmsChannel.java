package com.resilire.notification.infrastructure.channel;

import com.resilire.notification.application.NotificationChannel;
import com.resilire.notification.domain.notification.Channel;
import com.resilire.notification.domain.notification.NotificationContent;
import com.resilire.notification.domain.notification.TravellerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** SMS channel — Phase 1 stand-in for the SMS/WhatsApp adapter (logs instead of sending). */
@Component
class SmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    @Override
    public Channel channel() {
        return Channel.SMS;
    }

    @Override
    public void send(TravellerId recipient, NotificationContent content) {
        log.info("SMS to {}: {}", recipient.value(), content.title());
    }
}
