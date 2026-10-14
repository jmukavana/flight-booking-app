package com.resilire.payments.application;

import com.resilire.payments.domain.payment.LedgerEntry;
import com.resilire.payments.domain.payment.LedgerEntryId;
import com.resilire.payments.domain.payment.LedgerRepository;
import com.resilire.payments.domain.payment.PaymentDomainEvent;
import com.resilire.payments.domain.payment.PaymentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disburses EU261/UK261/CANADA_APPR compensation to passengers. Idempotent on
 * {@code claimId}: a claim submitted twice returns the original payout entry so
 * the passenger is never paid twice (SDD §5.3, driver 1 "correctness of money").
 *
 * <p>Disbursement uses the same processor gateway as booking captures — the
 * processor decides whether to push to IBAN, card reversal, or digital wallet
 * based on the traveller's stored payment method.
 */
@Service
public class CompensationPayoutService {

    private static final Logger log = LoggerFactory.getLogger(CompensationPayoutService.class);

    private final LedgerRepository ledger;
    private final ProcessorGateway processor;
    private final IntegrationEventPublisher events;

    public CompensationPayoutService(LedgerRepository ledger, ProcessorGateway processor,
                                     IntegrationEventPublisher events) {
        this.ledger = ledger;
        this.processor = processor;
        this.events = events;
    }

    @Transactional
    public LedgerEntry payout(PayoutCommand command) {
        return ledger.findByIdempotencyKeyAndType(command.claimId(), PaymentType.PAYOUT)
                .orElseGet(() -> recordPayout(command));
    }

    private LedgerEntry recordPayout(PayoutCommand command) {
        String processorRef = processor.capture(command.claimId(), command.amount());
        LedgerEntry entry = LedgerEntry.payout(LedgerEntryId.newId(), command.traveller(),
                command.amount(), processorRef, command.claimId());
        ledger.append(entry);
        events.publish(new PaymentDomainEvent.PaymentCaptured(
                entry.processorRef(), entry.traveller(), entry.amount(),
                entry.idempotencyKey(), entry.recordedAt()));
        log.info("Compensation payout {} → traveller {} amount {}",
                processorRef, command.traveller().value(), command.amount());
        return entry;
    }
}
