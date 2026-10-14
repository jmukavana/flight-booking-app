package com.resilire.payments.infrastructure.persistence;

import com.resilire.payments.domain.payment.LedgerEntry;
import com.resilire.payments.domain.payment.LedgerEntryId;
import com.resilire.payments.domain.payment.LedgerRepository;
import com.resilire.payments.domain.payment.Money;
import com.resilire.payments.domain.payment.PaymentType;
import com.resilire.payments.domain.payment.TravellerId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter implementing the domain port. Translates between the
 * append-only {@link LedgerEntryRecord} rows and {@link LedgerEntry}; {@code append}
 * only ever inserts.
 */
@Repository
public class JpaLedgerRepository implements LedgerRepository {

    private final LedgerEntryJpaRepository rows;

    public JpaLedgerRepository(LedgerEntryJpaRepository rows) {
        this.rows = rows;
    }

    @Override
    public void append(LedgerEntry entry) {
        rows.save(toRow(entry));
    }

    @Override
    public Optional<LedgerEntry> findByIdempotencyKeyAndType(String idempotencyKey, PaymentType type) {
        return rows.findByIdempotencyKeyAndType(idempotencyKey, type).map(this::toDomain);
    }

    @Override
    public Optional<LedgerEntry> findCaptureByProcessorRef(String processorRef) {
        return rows.findByProcessorRefAndType(processorRef, PaymentType.CAPTURE).map(this::toDomain);
    }

    private LedgerEntryRecord toRow(LedgerEntry entry) {
        return new LedgerEntryRecord(
                entry.id().value(),
                entry.type(),
                entry.traveller().value(),
                entry.amount().amount(),
                entry.amount().currency(),
                entry.processorRef(),
                entry.idempotencyKey(),
                entry.relatedRef(),
                entry.recordedAt());
    }

    private LedgerEntry toDomain(LedgerEntryRecord row) {
        return new LedgerEntry(
                LedgerEntryId.of(row.getId()),
                row.getType(),
                TravellerId.of(row.getTravellerId()),
                new Money(row.getAmount(), row.getCurrency()),
                row.getProcessorRef(),
                row.getIdempotencyKey(),
                row.getRelatedRef(),
                row.getRecordedAt());
    }
}
