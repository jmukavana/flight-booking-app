package com.resilire.itinerary.domain.itinerary;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The unified itinerary read model (CQRS read side, ADR-5): a denormalised
 * projection folded from order (and, later, recovery) events, served
 * upstream-independently from Redis (NFR-AVL-1).
 *
 * <p>{@code lastEventAt} is the projection's high-water mark. Because the outbox
 * delivers at-least-once and across two topics, the projection must be idempotent
 * and tolerate redelivery/reordering: {@link #supersededBy(Instant)} lets the
 * application drop any event not strictly newer than what has already been applied.
 */
public final class Itinerary {

    private final ItineraryId id;
    private final TravellerId traveller;
    private ItineraryStatus status;
    private final List<ItineraryLeg> legs;
    private final Money totalAmount;
    private final Instant bookedAt;
    private Instant lastEventAt;
    private ResilienceScore score;
    private Instant lastScoredAt;

    private Itinerary(ItineraryId id, TravellerId traveller, ItineraryStatus status,
                      List<ItineraryLeg> legs, Money totalAmount, Instant bookedAt, Instant lastEventAt,
                      ResilienceScore score, Instant lastScoredAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.traveller = Objects.requireNonNull(traveller, "traveller");
        this.status = Objects.requireNonNull(status, "status");
        this.legs = List.copyOf(legs);
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount");
        this.bookedAt = Objects.requireNonNull(bookedAt, "bookedAt");
        this.lastEventAt = Objects.requireNonNull(lastEventAt, "lastEventAt");
        this.score = score;
        this.lastScoredAt = lastScoredAt;
    }

    /** Build the projection from a confirmed order. The score arrives later, on its own event. */
    public static Itinerary placed(ItineraryId id, TravellerId traveller, List<ItineraryLeg> legs,
                                   Money totalAmount, Instant bookedAt, Instant eventAt) {
        return new Itinerary(id, traveller, ItineraryStatus.CONFIRMED, legs, totalAmount, bookedAt,
                eventAt, null, null);
    }

    /** Rebuild from the persisted projection. */
    public static Itinerary rehydrate(ItineraryId id, TravellerId traveller, ItineraryStatus status,
                                      List<ItineraryLeg> legs, Money totalAmount, Instant bookedAt,
                                      Instant lastEventAt, ResilienceScore score, Instant lastScoredAt) {
        return new Itinerary(id, traveller, status, legs, totalAmount, bookedAt, lastEventAt,
                score, lastScoredAt);
    }

    public void cancel(Instant eventAt) {
        this.status = ItineraryStatus.CANCELLED;
        this.lastEventAt = eventAt;
    }

    /** Attach (or refresh) the resilience score from a {@code ScoreComputed} event. */
    public void attachScore(ResilienceScore score) {
        this.score = score;
        this.lastScoredAt = score.scoredAt();
    }

    /**
     * @return true if {@code eventAt} is not strictly newer than the last applied
     *         <em>order</em> event — a duplicate or out-of-order delivery to ignore.
     */
    public boolean supersededBy(Instant eventAt) {
        return !eventAt.isAfter(lastEventAt);
    }

    /**
     * @return true if {@code scoredAt} is not strictly newer than the last applied
     *         score. Tracked separately from {@link #supersededBy} because scores
     *         arrive on their own topic with an independent timeline.
     */
    public boolean scoreSupersededBy(Instant scoredAt) {
        return lastScoredAt != null && !scoredAt.isAfter(lastScoredAt);
    }

    /**
     * Phase 3 (FR-7): true when the trip contains legs on different PNRs, meaning
     * legs were booked on separate tickets. Separate-ticket trips have no carrier
     * misconnection protection — a disruption to leg 1 does not trigger automatic
     * rebooking of leg 2.
     */
    public boolean hasSeparateTicketRisk() {
        if (legs.size() <= 1) return false;
        String firstPnr = legs.get(0).pnr();
        return legs.stream().anyMatch(l -> !l.pnr().equals(firstPnr));
    }

    public ItineraryId id() {
        return id;
    }

    public TravellerId traveller() {
        return traveller;
    }

    public ItineraryStatus status() {
        return status;
    }

    public List<ItineraryLeg> legs() {
        return legs;
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public Instant bookedAt() {
        return bookedAt;
    }

    public Instant lastEventAt() {
        return lastEventAt;
    }

    /** The attached resilience score, or {@code null} if it has not arrived yet. */
    public ResilienceScore score() {
        return score;
    }

    public Instant lastScoredAt() {
        return lastScoredAt;
    }
}
