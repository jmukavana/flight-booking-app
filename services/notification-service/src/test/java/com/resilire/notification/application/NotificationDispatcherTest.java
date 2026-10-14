package com.resilire.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.resilire.notification.domain.notification.Channel;
import com.resilire.notification.domain.notification.ChannelDeliveryException;
import com.resilire.notification.domain.notification.DeliveryResult;
import com.resilire.notification.domain.notification.DeliveryStatus;
import com.resilire.notification.domain.notification.NotificationContent;
import com.resilire.notification.domain.notification.NotificationMessage;
import com.resilire.notification.domain.notification.NotificationPreferences;
import com.resilire.notification.domain.notification.NotificationPriority;
import com.resilire.notification.domain.notification.NotificationType;
import com.resilire.notification.domain.notification.QuietHours;
import com.resilire.notification.domain.notification.TravellerId;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Dispatch policy: quiet-hours suppression, urgent bypass, and the channel fallback chain. */
class NotificationDispatcherTest {

    private final TravellerId traveller = TravellerId.of("traveller-1");
    private final FakePublisher publisher = new FakePublisher();

    @Test
    void delivers_via_the_first_preferred_channel() {
        RecordingChannel email = new RecordingChannel(Channel.EMAIL, true);
        RecordingChannel sms = new RecordingChannel(Channel.SMS, true);
        NotificationDispatcher dispatcher = dispatcher(prefs(QuietHours.none(), Channel.EMAIL, Channel.SMS),
                email, sms);

        DeliveryResult result = dispatcher.dispatch(confirmed());

        assertThat(result.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(result.channel()).isEqualTo(Channel.EMAIL);
        assertThat(email.attempts).isEqualTo(1);
        assertThat(sms.attempts).isZero();
    }

    @Test
    void falls_back_to_the_next_channel_when_the_first_fails() {
        RecordingChannel email = new RecordingChannel(Channel.EMAIL, false);
        RecordingChannel sms = new RecordingChannel(Channel.SMS, true);
        NotificationDispatcher dispatcher = dispatcher(prefs(QuietHours.none(), Channel.EMAIL, Channel.SMS),
                email, sms);

        DeliveryResult result = dispatcher.dispatch(confirmed());

        assertThat(result.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(result.channel()).isEqualTo(Channel.SMS);
        assertThat(email.attempts).isEqualTo(1);
        assertThat(sms.attempts).isEqualTo(1);
    }

    @Test
    void reports_failed_when_every_channel_fails() {
        NotificationDispatcher dispatcher = dispatcher(prefs(QuietHours.none(), Channel.EMAIL, Channel.SMS),
                new RecordingChannel(Channel.EMAIL, false), new RecordingChannel(Channel.SMS, false));

        DeliveryResult result = dispatcher.dispatch(confirmed());

        assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(result.channel()).isNull();
    }

    @Test
    void suppresses_normal_notifications_during_quiet_hours() {
        RecordingChannel email = new RecordingChannel(Channel.EMAIL, true);
        NotificationDispatcher dispatcher = dispatcher(prefs(allDayQuiet(), Channel.EMAIL), email);

        DeliveryResult result = dispatcher.dispatch(confirmed());

        assertThat(result.status()).isEqualTo(DeliveryStatus.SUPPRESSED);
        assertThat(email.attempts).isZero();
    }

    @Test
    void urgent_notifications_bypass_quiet_hours() {
        RecordingChannel email = new RecordingChannel(Channel.EMAIL, true);
        NotificationDispatcher dispatcher = dispatcher(prefs(allDayQuiet(), Channel.EMAIL), email);

        DeliveryResult result = dispatcher.dispatch(urgent());

        assertThat(result.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(email.attempts).isEqualTo(1);
    }

    private NotificationDispatcher dispatcher(NotificationPreferences prefs, NotificationChannel... channels) {
        return new NotificationDispatcher(List.of(channels), t -> prefs, publisher);
    }

    private NotificationPreferences prefs(QuietHours quietHours, Channel... order) {
        return new NotificationPreferences(List.of(order), quietHours);
    }

    private QuietHours allDayQuiet() {
        return QuietHours.of(LocalTime.MIN, LocalTime.MAX, ZoneOffset.UTC);
    }

    private NotificationMessage confirmed() {
        return NotificationMessage.orderConfirmed(traveller, "ABC123", "LHR", "JFK");
    }

    private NotificationMessage urgent() {
        return new NotificationMessage(
                com.resilire.notification.domain.notification.NotificationId.newId(), traveller,
                NotificationType.ORDER_CANCELLED, NotificationPriority.URGENT,
                new NotificationContent("Disruption", "Your flight is disrupted"), java.time.Instant.now());
    }

    private static final class RecordingChannel implements NotificationChannel {
        private final Channel channel;
        private final boolean succeeds;
        private int attempts;

        RecordingChannel(Channel channel, boolean succeeds) {
            this.channel = channel;
            this.succeeds = succeeds;
        }

        @Override
        public Channel channel() {
            return channel;
        }

        @Override
        public void send(TravellerId recipient, NotificationContent content) {
            attempts++;
            if (!succeeds) {
                throw new ChannelDeliveryException(channel, "test failure");
            }
        }
    }

    private static final class FakePublisher implements DeliveryEventPublisher {
        private final List<DeliveryResult> published = new ArrayList<>();

        @Override
        public void publish(DeliveryResult result) {
            published.add(result);
        }
    }
}
