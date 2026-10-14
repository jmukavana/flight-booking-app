package com.resilire.identityconsent.application;

import com.resilire.identityconsent.domain.consent.ConsentDomainEvent;

/**
 * Outbound port: turns a domain event into the {@code ConsentChanged} integration
 * event for other bounded contexts. The implementation MUST write within the
 * same transaction as the consent entries (transactional outbox, ADR-10) so the
 * fact and its announcement commit atomically.
 */
public interface IntegrationEventPublisher {

    void publish(ConsentDomainEvent event);
}
