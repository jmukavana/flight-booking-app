package com.resilire.itinerary.infrastructure.web;

import com.resilire.itinerary.application.ItineraryQueryService;
import com.resilire.itinerary.domain.itinerary.Itinerary;
import com.resilire.itinerary.domain.itinerary.ItineraryId;
import com.resilire.itinerary.domain.itinerary.ItineraryLeg;
import com.resilire.itinerary.domain.itinerary.ResilienceScore;
import com.resilire.itinerary.domain.itinerary.TravellerId;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound adapter for the unified itinerary read model. Reads are always scoped to
 * the authenticated OIDC subject — a Traveller can only see their own itineraries.
 */
@RestController
@RequestMapping("/itineraries")
public class ItineraryController {

    private final ItineraryQueryService itineraries;

    public ItineraryController(ItineraryQueryService itineraries) {
        this.itineraries = itineraries;
    }

    @GetMapping
    public List<ItineraryView> list(@AuthenticationPrincipal Jwt jwt) {
        return itineraries.listForTraveller(currentTraveller(jwt)).stream()
                .map(ItineraryView::of)
                .toList();
    }

    @GetMapping("/{id}")
    public ItineraryView get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ItineraryView.of(itineraries.getItinerary(currentTraveller(jwt), ItineraryId.of(id)));
    }

    private TravellerId currentTraveller(Jwt jwt) {
        return TravellerId.of(jwt.getSubject());
    }

    public record ItineraryView(String itineraryId, String status, List<LegView> legs,
                                String amount, String currency, Instant bookedAt, Instant lastUpdatedAt,
                                ScoreView resilienceScore, boolean separateTicketRisk) {
        static ItineraryView of(Itinerary it) {
            return new ItineraryView(
                    it.id().value().toString(),
                    it.status().name(),
                    it.legs().stream().map(LegView::of).toList(),
                    it.totalAmount().amount().toPlainString(),
                    it.totalAmount().currency(),
                    it.bookedAt(),
                    it.lastEventAt(),
                    it.score() == null ? null : ScoreView.of(it.score()),
                    it.hasSeparateTicketRisk());
        }
    }

    /** Null until the {@code ScoreComputed} event arrives; {@code source} flags a degraded score. */
    public record ScoreView(int value, String band, String source, String modelVersion, Instant scoredAt) {
        static ScoreView of(ResilienceScore s) {
            return new ScoreView(s.value(), s.band(), s.source(), s.modelVersion(), s.scoredAt());
        }
    }

    public record LegView(String origin, String destination, Instant departure, Instant arrival,
                          String marketingCarrier, String pnr) {
        static LegView of(ItineraryLeg leg) {
            return new LegView(leg.origin(), leg.destination(), leg.departure(), leg.arrival(),
                    leg.marketingCarrier(), leg.pnr());
        }
    }
}
