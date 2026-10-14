package com.resilire.payments.application;

import com.resilire.payments.domain.payment.LedgerEntry;
import com.resilire.payments.domain.payment.LedgerRepository;
import com.resilire.payments.domain.payment.Money;
import com.resilire.payments.domain.payment.PaymentDomainEvent;
import com.resilire.payments.domain.payment.PaymentType;
import com.resilire.payments.domain.payment.TravellerId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompensationPayoutServiceTest {

    private final TravellerId traveller = TravellerId.of("traveller-1");
    private final Money amount = Money.of("250.00", "EUR");

    private InMemoryLedger ledger;
    private FakeProcessor processor;
    private FakePublisher publisher;
    private CompensationPayoutService service;

    @BeforeEach
    void setUp() {
        ledger = new InMemoryLedger();
        processor = new FakeProcessor();
        publisher = new FakePublisher();
        service = new CompensationPayoutService(ledger, processor, publisher);
    }

    @Test
    void payout_records_payout_entry_and_publishes_event() {
        LedgerEntry entry = service.payout(new PayoutCommand("claim-1", traveller, amount));

        assertThat(entry.type()).isEqualTo(PaymentType.PAYOUT);
        assertThat(entry.traveller()).isEqualTo(traveller);
        assertThat(entry.amount()).isEqualTo(amount);
        assertThat(entry.idempotencyKey()).isEqualTo("claim-1");
        assertThat(entry.processorRef()).startsWith("ch_");
        assertThat(publisher.events).singleElement().isInstanceOf(PaymentDomainEvent.PaymentCaptured.class);
    }

    @Test
    void payout_is_idempotent_on_claim_id() {
        LedgerEntry first = service.payout(new PayoutCommand("claim-2", traveller, amount));
        LedgerEntry second = service.payout(new PayoutCommand("claim-2", traveller, amount));

        assertThat(second.processorRef()).isEqualTo(first.processorRef());
        assertThat(processor.captureCalls).isEqualTo(1);
        assertThat(ledger.entries).hasSize(1);
    }

    @Test
    void payout_and_booking_capture_use_independent_idempotency_keys() {
        service.payout(new PayoutCommand("claim-3", traveller, amount));
        service.payout(new PayoutCommand("claim-4", traveller, amount));

        assertThat(ledger.entries).hasSize(2);
        assertThat(ledger.entries).allMatch(e -> e.type() == PaymentType.PAYOUT);
    }

    @Test
    void payout_does_not_affect_refund_lookups() {
        service.payout(new PayoutCommand("claim-5", traveller, amount));

        Optional<LedgerEntry> refundLookup = ledger.findByIdempotencyKeyAndType("claim-5", PaymentType.REFUND);
        assertThat(refundLookup).isEmpty();
    }

    private static final class InMemoryLedger implements LedgerRepository {
        final List<LedgerEntry> entries = new ArrayList<>();

        @Override public void append(LedgerEntry entry) { entries.add(entry); }

        @Override public Optional<LedgerEntry> findByIdempotencyKeyAndType(String key, PaymentType type) {
            return entries.stream()
                    .filter(e -> e.idempotencyKey().equals(key) && e.type() == type)
                    .findFirst();
        }

        @Override public Optional<LedgerEntry> findCaptureByProcessorRef(String ref) {
            return entries.stream()
                    .filter(e -> e.type() == PaymentType.CAPTURE && e.processorRef().equals(ref))
                    .findFirst();
        }
    }

    private static final class FakeProcessor implements ProcessorGateway {
        int captureCalls = 0;

        @Override public String capture(String key, Money amount) { return "ch_" + (++captureCalls); }
        @Override public String refund(String key, String ref, Money amount) { return "re_1"; }
    }

    private static final class FakePublisher implements IntegrationEventPublisher {
        final List<PaymentDomainEvent> events = new ArrayList<>();

        @Override public void publish(PaymentDomainEvent event) { events.add(event); }
    }
}
