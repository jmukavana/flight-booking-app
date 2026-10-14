package com.resilire.itinerary.domain.itinerary;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Read-model invariants — pure domain, no Spring, fast. */
class ItineraryTest {

    private final ItineraryId id = ItineraryId.of(UUID.randomUUID());
    private final TravellerId traveller = TravellerId.of("traveller-1");
    private final Instant booked = Instant.parse("2026-07-01T07:00:00Z");

    @Test
    void placed_is_confirmed_with_its_leg() {
        Itinerary itinerary = placed(booked);

        assertThat(itinerary.status()).isEqualTo(ItineraryStatus.CONFIRMED);
        assertThat(itinerary.legs()).singleElement()
                .extracting(ItineraryLeg::destination).isEqualTo("JFK");
        assertThat(itinerary.lastEventAt()).isEqualTo(booked);
    }

    @Test
    void cancel_sets_cancelled_and_advances_high_water_mark() {
        Itinerary itinerary = placed(booked);
        Instant cancelledAt = booked.plusSeconds(3600);

        itinerary.cancel(cancelledAt);

        assertThat(itinerary.status()).isEqualTo(ItineraryStatus.CANCELLED);
        assertThat(itinerary.lastEventAt()).isEqualTo(cancelledAt);
    }

    @Test
    void supersededBy_is_true_for_events_not_strictly_newer() {
        Itinerary itinerary = placed(booked);

        assertThat(itinerary.supersededBy(booked)).isTrue();
        assertThat(itinerary.supersededBy(booked.minusSeconds(1))).isTrue();
        assertThat(itinerary.supersededBy(booked.plusSeconds(1))).isFalse();
    }

    @Test
    void attachScore_sets_the_score_and_its_own_high_water_mark() {
        Itinerary itinerary = placed(booked);
        Instant scoredAt = booked.plusSeconds(5);
        ResilienceScore score = new ResilienceScore(82, "RESILIENT", "MODEL", "model-v1", scoredAt);

        itinerary.attachScore(score);

        assertThat(itinerary.score()).isEqualTo(score);
        assertThat(itinerary.lastScoredAt()).isEqualTo(scoredAt);
    }

    @Test
    void score_high_water_mark_is_independent_of_the_order_timeline() {
        Itinerary itinerary = placed(booked);

        // No score applied yet: nothing can be superseded.
        assertThat(itinerary.scoreSupersededBy(booked.plusSeconds(10))).isFalse();

        Instant scoredAt = booked.plusSeconds(5);
        itinerary.attachScore(new ResilienceScore(82, "RESILIENT", "MODEL", "model-v1", scoredAt));

        assertThat(itinerary.scoreSupersededBy(scoredAt)).isTrue();
        assertThat(itinerary.scoreSupersededBy(scoredAt.plusSeconds(1))).isFalse();
    }

    @Test
    void single_leg_itinerary_has_no_separate_ticket_risk() {
        Itinerary itinerary = placed(booked);

        assertThat(itinerary.hasSeparateTicketRisk()).isFalse();
    }

    @Test
    void multi_leg_with_different_pnrs_has_separate_ticket_risk() {
        ItineraryLeg leg1 = new ItineraryLeg("NBO", "LHR",
                booked, booked.plusSeconds(28_800), "KQ", "PNR001");
        ItineraryLeg leg2 = new ItineraryLeg("LHR", "JFK",
                booked.plusSeconds(32_400), booked.plusSeconds(57_600), "BA", "PNR002");
        Itinerary itinerary = Itinerary.placed(id, traveller, List.of(leg1, leg2),
                Money.of("350.00", "EUR"), booked, booked);

        assertThat(itinerary.hasSeparateTicketRisk()).isTrue();
    }

    @Test
    void multi_leg_with_same_pnr_has_no_separate_ticket_risk() {
        ItineraryLeg leg1 = new ItineraryLeg("NBO", "LHR",
                booked, booked.plusSeconds(28_800), "KQ", "SAMEPNR");
        ItineraryLeg leg2 = new ItineraryLeg("LHR", "JFK",
                booked.plusSeconds(32_400), booked.plusSeconds(57_600), "KQ", "SAMEPNR");
        Itinerary itinerary = Itinerary.placed(id, traveller, List.of(leg1, leg2),
                Money.of("350.00", "EUR"), booked, booked);

        assertThat(itinerary.hasSeparateTicketRisk()).isFalse();
    }

    private Itinerary placed(Instant eventAt) {
        ItineraryLeg leg = new ItineraryLeg("LHR", "JFK",
                eventAt, eventAt.plusSeconds(25_200), "STUB-AIR", "ABC123");
        return Itinerary.placed(id, traveller, List.of(leg), Money.of("189.00", "EUR"), eventAt, eventAt);
    }
}
