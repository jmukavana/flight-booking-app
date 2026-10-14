package com.resilire.notification.infrastructure.messaging;

import com.resilire.notification.application.NotificationDispatcher;
import com.resilire.notification.domain.notification.NotificationMessage;
import com.resilire.notification.domain.notification.TravellerId;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Inbound adapter: turns Booking's order events into Traveller notifications.
 * Delivery is at-least-once; a redelivered event resends, so consumers of
 * {@code NotificationDelivered} dedupe on the notification id (a dedup store here
 * is Phase 2 hardening).
 */
@Component
public class OrderEventListener {

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper json;

    public OrderEventListener(NotificationDispatcher dispatcher, ObjectMapper json) {
        this.dispatcher = dispatcher;
        this.json = json;
    }

    @KafkaListener(topics = "order.created")
    public void onOrderCreated(String payload) {
        OrderCreatedEvent event = deserialize(payload, OrderCreatedEvent.class);
        dispatcher.dispatch(NotificationMessage.orderConfirmed(
                TravellerId.of(event.travellerId()), event.pnr(), event.origin(), event.destination()));
    }

    @KafkaListener(topics = "order.cancelled")
    public void onOrderCancelled(String payload) {
        OrderCancelledEvent event = deserialize(payload, OrderCancelledEvent.class);
        dispatcher.dispatch(NotificationMessage.orderCancelled(
                TravellerId.of(event.travellerId()), event.orderId()));
    }

    private <T> T deserialize(String payload, Class<T> type) {
        try {
            return json.readValue(payload, type);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to deserialize " + type.getSimpleName(), e);
        }
    }
}
