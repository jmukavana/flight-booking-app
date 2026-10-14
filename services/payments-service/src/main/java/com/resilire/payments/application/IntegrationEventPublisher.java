package com.resilire.payments.application;

import com.resilire.payments.domain.payment.PaymentDomainEvent;

/**
 * Outbound port: turns a domain event into the {@code PaymentCaptured}/
 * {@code PaymentRefunded} integration event. The implementation MUST write within
 * the same transaction as the ledger entry (transactional outbox, ADR-10) so the
 * money movement and its announcement commit atomically — the immutable audit log.
 */
public interface IntegrationEventPublisher {

    void publish(PaymentDomainEvent event);
}
