package com.resilire.booking.application;

import com.resilire.booking.domain.order.OrderDomainEvent;

/**
 * Outbound port: turns a domain event into the {@code OrderCreated}/
 * {@code OrderCancelled} integration event for other bounded contexts. The
 * implementation MUST write within the same transaction as the order
 * (transactional outbox, ADR-10) so the fact and its announcement commit atomically.
 */
public interface IntegrationEventPublisher {

    void publish(OrderDomainEvent event);
}
