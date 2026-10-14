package com.resilire.payments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resilire.payments.domain.payment.LedgerEntry;
import com.resilire.payments.domain.payment.LedgerRepository;
import com.resilire.payments.domain.payment.Money;
import com.resilire.payments.domain.payment.PaymentDomainEvent;
import com.resilire.payments.domain.payment.PaymentNotFoundException;
import com.resilire.payments.domain.payment.PaymentType;
import com.resilire.payments.domain.payment.TravellerId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Money-critical paths: idempotent capture/refund, ownership, and event emission. */
class PaymentApplicationServiceTest {

    private final TravellerId traveller = TravellerId.of("traveller-1");
    private final Money amount = Money.of("189.00", "EUR");

    private InMemoryLedger ledger;
    private FakeProcessor processor;
    private FakePublisher publisher;
    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        ledger = new InMemoryLedger();
        processor = new FakeProcessor();
        publisher = new FakePublisher();
        service = new PaymentApplicationService(ledger, processor, publisher);
    }

    @Test
    void capture_records_entry_and_publishes_event() {
        LedgerEntry entry = service.capture(new CaptureCommand(traveller, amount, "key-1"));

        assertThat(entry.type()).isEqualTo(PaymentType.CAPTURE);
        assertThat(entry.processorRef()).startsWith("ch_");
        assertThat(publisher.events).singleElement().isInstanceOf(PaymentDomainEvent.PaymentCaptured.class);
    }

    @Test
    void capture_is_idempotent_on_the_key() {
        LedgerEntry first = service.capture(new CaptureCommand(traveller, amount, "key-1"));
        LedgerEntry second = service.capture(new CaptureCommand(traveller, amount, "key-1"));

        assertThat(second.processorRef()).isEqualTo(first.processorRef());
        assertThat(processor.captureCalls).isEqualTo(1);
        assertThat(ledger.entries).hasSize(1);
    }

    @Test
    void refund_records_entry_for_an_existing_capture() {
        LedgerEntry capture = service.capture(new CaptureCommand(traveller, amount, "key-1"));

        LedgerEntry refund = service.refund(new RefundCommand(traveller, capture.processorRef(), "key-1"));

        assertThat(refund.type()).isEqualTo(PaymentType.REFUND);
        assertThat(refund.relatedRef()).isEqualTo(capture.processorRef());
        assertThat(refund.amount()).isEqualTo(amount);
        assertThat(publisher.events).last().isInstanceOf(PaymentDomainEvent.PaymentRefunded.class);
    }

    @Test
    void refund_is_idempotent_on_the_key() {
        LedgerEntry capture = service.capture(new CaptureCommand(traveller, amount, "key-1"));
        LedgerEntry first = service.refund(new RefundCommand(traveller, capture.processorRef(), "key-1"));
        LedgerEntry second = service.refund(new RefundCommand(traveller, capture.processorRef(), "key-1"));

        assertThat(second.processorRef()).isEqualTo(first.processorRef());
        assertThat(processor.refundCalls).isEqualTo(1);
    }

    @Test
    void refund_of_an_unknown_capture_is_rejected() {
        assertThatThrownBy(() -> service.refund(new RefundCommand(traveller, "ch_missing", "key-1")))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void refund_by_a_different_traveller_is_rejected() {
        LedgerEntry capture = service.capture(new CaptureCommand(traveller, amount, "key-1"));

        assertThatThrownBy(() -> service.refund(
                new RefundCommand(TravellerId.of("intruder"), capture.processorRef(), "key-2")))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    private static final class InMemoryLedger implements LedgerRepository {
        private final List<LedgerEntry> entries = new ArrayList<>();

        @Override
        public void append(LedgerEntry entry) {
            entries.add(entry);
        }

        @Override
        public Optional<LedgerEntry> findByIdempotencyKeyAndType(String idempotencyKey, PaymentType type) {
            return entries.stream()
                    .filter(e -> e.idempotencyKey().equals(idempotencyKey) && e.type() == type)
                    .findFirst();
        }

        @Override
        public Optional<LedgerEntry> findCaptureByProcessorRef(String processorRef) {
            return entries.stream()
                    .filter(e -> e.type() == PaymentType.CAPTURE && e.processorRef().equals(processorRef))
                    .findFirst();
        }
    }

    private static final class FakeProcessor implements ProcessorGateway {
        private int captureCalls;
        private int refundCalls;

        @Override
        public String capture(String idempotencyKey, Money amount) {
            return "ch_" + (++captureCalls);
        }

        @Override
        public String refund(String idempotencyKey, String chargeRef, Money amount) {
            return "re_" + (++refundCalls);
        }
    }

    private static final class FakePublisher implements IntegrationEventPublisher {
        private final List<PaymentDomainEvent> events = new ArrayList<>();

        @Override
        public void publish(PaymentDomainEvent event) {
            events.add(event);
        }
    }
}
