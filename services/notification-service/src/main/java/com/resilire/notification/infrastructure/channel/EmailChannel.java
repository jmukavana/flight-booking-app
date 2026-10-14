package com.resilire.notification.infrastructure.channel;

import com.resilire.notification.application.NotificationChannel;
import com.resilire.notification.domain.notification.Channel;
import com.resilire.notification.domain.notification.NotificationContent;
import com.resilire.notification.domain.notification.TravellerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Email channel. Phase 1 stand-in for the SMTP adapter — it logs the send rather
 * than transmitting, because the flight-booking flow has no recipient email address
 * yet (the event carries the OIDC subject, not an address; resolving it is the
 * identity-service integration). The SMTP detail ({@code resilire.notification.email.*})
 * is wired so a real JavaMailSender adapter drops in unchanged; the password is read
 * from the {@code RESILIRE_MAIL_PASSWORD} env var, never hardcoded.
 */
@Component
class EmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    private final String host;
    private final String username;
    private final String fromAddress;

    EmailChannel(@Value("${resilire.notification.email.host:localhost}") String host,
                 @Value("${resilire.notification.email.username:no-reply@resilire.app}") String username,
                 @Value("${resilire.notification.email.from:no-reply@resilire.app}") String fromAddress) {
        this.host = host;
        this.username = username;
        this.fromAddress = fromAddress;
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public void send(TravellerId recipient, NotificationContent content) {
        log.info("EMAIL via {} ({}) from {} to traveller {}: {}",
                host, username, fromAddress, recipient.value(), content.title());
    }
}
