package com.resilire.itinerary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.resilire.itinerary.domain.itinerary.Itinerary;
import com.resilire.itinerary.domain.itinerary.ItineraryId;
import com.resilire.itinerary.domain.itinerary.ItineraryLeg;
import com.resilire.itinerary.domain.itinerary.ItineraryRepository;
import com.resilire.itinerary.domain.itinerary.ItineraryStatus;
import com.resilire.itinerary.domain.itinerary.Money;
import com.resilire.itinerary.domain.itinerary.ResilienceScore;
import com.resilire.itinerary.domain.itinerary.TravellerId;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Projection idempotency + ordering guards, exercised against an in-memory repo. */
class ItineraryProjectionServiceTest {

    private final ItineraryId id = ItineraryId.of(UUID.randomUUID());
    private final TravellerId traveller = TravellerId.of("traveller-1");
    private final Instant placedAt = Instant.parse("2026-07-01T07:00:00Z");

    private InMemoryItineraryRepository repo;
    private ItineraryProjectionService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryItineraryRepository();
        service = new ItineraryProjectionService(repo);
    }

    @Test
    void order_placed_creates_a_confirmed_itinerary() {
        service.onOrderPlaced(placed(placedAt));

        Itinerary stored = repo.findById(id).orElseThrow();
        assertThat(stored.status()).isEqualTo(ItineraryStatus.CONFIRMED);
        assertThat(stored.traveller()).isEqualTo(traveller);
    }

    @Test
    void redelivered_order_placed_does_not_overwrite_with_stale_data() {
        service.onOrderPlaced(placed(placedAt));
        service.onOrderPlaced(placed(placedAt)); // same occurredAt = redelivery

        assertThat(repo.saveCount).isEqualTo(1);
    }

    @Test
    void order_cancelled_after_placed_cancels_the_itinerary() {
        service.onOrderPlaced(placed(placedAt));
        service.onOrderCancelled(new OrderCancelledProjection(id, placedAt.plusSeconds(3600)));

        assertThat(repo.findById(id).orElseThrow().status()).isEqualTo(ItineraryStatus.CANCELLED);
    }

    @Test
    void order_cancelled_for_unknown_itinerary_is_a_no_op() {
        service.onOrderCancelled(new OrderCancelledProjection(id, placedAt));

        assertThat(repo.findById(id)).isEmpty();
    }

    @Test
    void stale_cancel_does_not_change_an_already_advanced_itinerary() {
        service.onOrderPlaced(placed(placedAt));
        service.onOrderCancelled(new OrderCancelledProjection(id, placedAt.minusSeconds(1)));

        assertThat(repo.findById(id).orElseThrow().status()).isEqualTo(ItineraryStatus.CONFIRMED);
    }

    @Test
    void score_computed_attaches_the_score_to_an_existing_itinerary() {
        service.onOrderPlaced(placed(placedAt));
        service.onScoreComputed(new ScoreComputedProjection(id, score(82, placedAt.plusSeconds(5))));

        ResilienceScore stored = repo.findById(id).orElseThrow().score();
        assertThat(stored).isNotNull();
        assertThat(stored.value()).isEqualTo(82);
    }

    @Test
    void score_computed_for_unknown_itinerary_is_a_no_op() {
        service.onScoreComputed(new ScoreComputedProjection(id, score(82, placedAt)));

        assertThat(repo.findById(id)).isEmpty();
    }

    @Test
    void stale_score_does_not_overwrite_a_newer_one() {
        service.onOrderPlaced(placed(placedAt));
        service.onScoreComputed(new ScoreComputedProjection(id, score(90, placedAt.plusSeconds(10))));
        service.onScoreComputed(new ScoreComputedProjection(id, score(40, placedAt.plusSeconds(5))));

        assertThat(repo.findById(id).orElseThrow().score().value()).isEqualTo(90);
    }

    private ResilienceScore score(int value, Instant scoredAt) {
        return new ResilienceScore(value, "RESILIENT", "MODEL", "model-v1", scoredAt);
    }

    private OrderPlacedProjection placed(Instant occurredAt) {
        ItineraryLeg leg = new ItineraryLeg("LHR", "JFK",
                occurredAt, occurredAt.plusSeconds(25_200), "STUB-AIR", "ABC123");
        return new OrderPlacedProjection(id, traveller, leg, Money.of("189.00", "EUR"), occurredAt);
    }

    private static final class InMemoryItineraryRepository implements ItineraryRepository {
        private final Map<UUID, Itinerary> store = new HashMap<>();
        private int saveCount;

        @Override
        public void save(Itinerary itinerary) {
            saveCount++;
            store.put(itinerary.id().value(), itinerary);
        }

        @Override
        public Optional<Itinerary> findById(ItineraryId id) {
            return Optional.ofNullable(store.get(id.value()));
        }

        @Override
        public List<Itinerary> findByTraveller(TravellerId traveller) {
            return store.values().stream()
                    .filter(it -> it.traveller().equals(traveller))
                    .toList();
        }
    }
}
