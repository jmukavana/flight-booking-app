package com.resilire.notification.application;

import com.resilire.notification.domain.notification.Channel;
import com.resilire.notification.domain.notification.ChannelDeliveryException;
import com.resilire.notification.domain.notification.DeliveryPolicy;
import com.resilire.notification.domain.notification.DeliveryResult;
import com.resilire.notification.domain.notification.NotificationMessage;
import com.resilire.notification.domain.notification.NotificationPreferences;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Delivers a notification: apply the quiet-hours policy, then walk the Traveller's
 * channel fallback chain until one channel accepts it (SDD §3, FR-11). Every
 * outcome — delivered, suppressed, or failed on all channels — is published to the
 * delivery log.
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final Map<Channel, NotificationChannel> channels;
    private final PreferencesProvider preferences;
    private final DeliveryEventPublisher events;

    public NotificationDispatcher(List<NotificationChannel> channels,
                                  PreferencesProvider preferences,
                                  DeliveryEventPublisher events) {
        this.channels = new EnumMap<>(Channel.class);
        channels.forEach(channel -> this.channels.put(channel.channel(), channel));
        this.preferences = preferences;
        this.events = events;
    }

    public DeliveryResult dispatch(NotificationMessage message) {
        NotificationPreferences prefs = preferences.forTraveller(message.traveller());

        if (!DeliveryPolicy.shouldDeliver(prefs, message.priority(), Instant.now())) {
            log.debug("Suppressing {} for {} during quiet hours", message.type(), message.traveller().value());
            return record(DeliveryResult.suppressed(message, Instant.now()));
        }

        for (Channel channel : prefs.channelOrder()) {
            NotificationChannel sender = channels.get(channel);
            if (sender == null) {
                continue;
            }
            try {
                sender.send(message.traveller(), message.content());
                return record(DeliveryResult.delivered(message, channel, Instant.now()));
            } catch (ChannelDeliveryException e) {
                log.warn("Falling back from {} for {}: {}", channel, message.id().value(), e.getMessage());
            }
        }

        log.error("All channels failed for {} to {}", message.type(), message.traveller().value());
        return record(DeliveryResult.failed(message, Instant.now()));
    }

    private DeliveryResult record(DeliveryResult result) {
        events.publish(result);
        return result;
    }
}
