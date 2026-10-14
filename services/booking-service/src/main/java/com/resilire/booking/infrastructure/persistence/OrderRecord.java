package com.resilire.booking.infrastructure.persistence;

import com.resilire.booking.domain.order.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistence row for an {@link com.resilire.booking.domain.order.Order}. The
 * {@code idempotency_key} unique constraint is the last line of the exactly-once
 * guarantee (SDD §5.3): a racing duplicate placement becomes a constraint
 * violation rather than a second booking.
 */
@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_orders_idempotency_key",
                columnNames = "idempotency_key"))
public class OrderRecord {

    @Id
    private UUID id;

    @Column(name = "itinerary_id", nullable = false)
    private UUID itineraryId;

    @Column(name = "traveller_id", nullable = false)
    private String travellerId;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String pnr;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private Instant departure;

    @Column(nullable = false)
    private Instant arrival;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_ref", nullable = false)
    private String paymentRef;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    protected OrderRecord() {
        // for JPA
    }

    public OrderRecord(UUID id, UUID itineraryId, String travellerId, String provider, String pnr,
                       String origin, String destination, Instant departure, Instant arrival,
                       BigDecimal amount, String currency, String paymentRef, String idempotencyKey,
                       OrderStatus status, Instant placedAt) {
        this.id = id;
        this.itineraryId = itineraryId;
        this.travellerId = travellerId;
        this.provider = provider;
        this.pnr = pnr;
        this.origin = origin;
        this.destination = destination;
        this.departure = departure;
        this.arrival = arrival;
        this.amount = amount;
        this.currency = currency;
        this.paymentRef = paymentRef;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.placedAt = placedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getItineraryId() {
        return itineraryId;
    }

    public String getTravellerId() {
        return travellerId;
    }

    public String getProvider() {
        return provider;
    }

    public String getPnr() {
        return pnr;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public Instant getDeparture() {
        return departure;
    }

    public Instant getArrival() {
        return arrival;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }
}
