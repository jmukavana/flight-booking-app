package com.resilire.payments.application;

import com.resilire.payments.domain.payment.LedgerEntry;
import com.resilire.payments.domain.payment.LedgerEntryId;
import com.resilire.payments.domain.payment.LedgerRepository;
import com.resilire.payments.domain.payment.PaymentDomainEvent;
import com.resilire.payments.domain.payment.PaymentNotFoundException;
import com.resilire.payments.domain.payment.PaymentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the Payments context. Each command is one unit of work:
 * call the processor, append the immutable ledger entry, and publish its event
 * through the outbox — ledger and announcement commit together (ADR-10).
 *
 * <p>Exactly-once effect (SDD §5.3, driver 1 "correctness of money") rests on the
 * {@code idempotencyKey}: a retried capture or refund returns the original entry
 * instead of moving money again. The rare concurrent race is caught by the
 * {@code (idempotency_key, type)} unique constraint and mapped to 409-retry.
 */
@Service
public class PaymentApplicationService {

    private final LedgerRepository ledger;
    private final ProcessorGateway processor;
    private final IntegrationEventPublisher events;

    public PaymentApplicationService(LedgerRepository ledger, ProcessorGateway processor,
                                     IntegrationEventPublisher events) {
        this.ledger = ledger;
        this.processor = processor;
        this.events = events;
    }

    @Transactional
    public LedgerEntry capture(CaptureCommand command) {
        return ledger.findByIdempotencyKeyAndType(command.idempotencyKey(), PaymentType.CAPTURE)
                .orElseGet(() -> recordCapture(command));
    }

    private LedgerEntry recordCapture(CaptureCommand command) {
        String processorRef = processor.capture(command.idempotencyKey(), command.amount());
        LedgerEntry entry = LedgerEntry.capture(LedgerEntryId.newId(), command.traveller(),
                command.amount(), processorRef, command.idempotencyKey());
        return commit(entry);
    }

    @Transactional
    public LedgerEntry refund(RefundCommand command) {
        return ledger.findByIdempotencyKeyAndType(command.idempotencyKey(), PaymentType.REFUND)
                .orElseGet(() -> recordRefund(command));
    }

    private LedgerEntry recordRefund(RefundCommand command) {
        LedgerEntry capture = ledger.findCaptureByProcessorRef(command.capturedRef())
                .filter(entry -> entry.traveller().equals(command.traveller()))
                .orElseThrow(() -> new PaymentNotFoundException(command.capturedRef()));

        String processorRef = processor.refund(command.idempotencyKey(), capture.processorRef(), capture.amount());
        LedgerEntry entry = LedgerEntry.refund(LedgerEntryId.newId(), capture.traveller(),
                capture.amount(), processorRef, command.idempotencyKey(), capture.processorRef());
        return commit(entry);
    }

    private LedgerEntry commit(LedgerEntry entry) {
        ledger.append(entry);
        events.publish(PaymentDomainEvent.of(entry));
        return entry;
    }
}
