package com.resilire.itinerary.infrastructure.persistence;

import com.resilire.itinerary.domain.itinerary.Itinerary;
import com.resilire.itinerary.domain.itinerary.ItineraryId;
import com.resilire.itinerary.domain.itinerary.ItineraryLeg;
import com.resilire.itinerary.domain.itinerary.ItineraryStatus;
import com.resilire.itinerary.domain.itinerary.Money;
import com.resilire.itinerary.domain.itinerary.ResilienceScore;
import com.resilire.itinerary.domain.itinerary.TravellerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed adapter implementing the domain port. Translates between the
 * {@link ItineraryDocument} hash and the {@link Itinerary} read model. {@code save}
 * is an upsert keyed by the itinerary id.
 */
@Repository
public class RedisItineraryRepository implements com.resilire.itinerary.domain.itinerary.ItineraryRepository {

    private final ItineraryRedisRepository documents;

    public RedisItineraryRepository(ItineraryRedisRepository documents) {
        this.documents = documents;
    }

    @Override
    public void save(Itinerary itinerary) {
        documents.save(toDocument(itinerary));
    }

    @Override
    public Optional<Itinerary> findById(ItineraryId id) {
        return documents.findById(id.value().toString()).map(this::toDomain);
    }

    @Override
    public List<Itinerary> findByTraveller(TravellerId traveller) {
        return documents.findByTravellerId(traveller.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private ItineraryDocument toDocument(Itinerary itinerary) {
        List<LegDocument> legs = itinerary.legs().stream()
                .map(leg -> new LegDocument(leg.origin(), leg.destination(),
                        leg.departure().toEpochMilli(), leg.arrival().toEpochMilli(),
                        leg.marketingCarrier(), leg.pnr()))
                .toList();
        ResilienceScore score = itinerary.score();
        return new ItineraryDocument(
                itinerary.id().value().toString(),
                itinerary.traveller().value(),
                itinerary.status().name(),
                legs,
                itinerary.totalAmount().amount().toPlainString(),
                itinerary.totalAmount().currency(),
                itinerary.bookedAt().toEpochMilli(),
                itinerary.lastEventAt().toEpochMilli(),
                score == null ? null : score.value(),
                score == null ? null : score.band(),
                score == null ? null : score.source(),
                score == null ? null : score.modelVersion(),
                itinerary.lastScoredAt() == null ? null : itinerary.lastScoredAt().toEpochMilli());
    }

    private Itinerary toDomain(ItineraryDocument doc) {
        List<ItineraryLeg> legs = doc.getLegs().stream()
                .map(leg -> new ItineraryLeg(leg.getOrigin(), leg.getDestination(),
                        Instant.ofEpochMilli(leg.getDepartureEpochMs()),
                        Instant.ofEpochMilli(leg.getArrivalEpochMs()),
                        leg.getMarketingCarrier(), leg.getPnr()))
                .toList();
        Instant lastScoredAt = doc.getScoredAtEpochMs() == null ? null
                : Instant.ofEpochMilli(doc.getScoredAtEpochMs());
        ResilienceScore score = doc.getScoreValue() == null ? null
                : new ResilienceScore(doc.getScoreValue(), doc.getScoreBand(), doc.getScoreSource(),
                        doc.getScoreModelVersion(), lastScoredAt);
        return Itinerary.rehydrate(
                ItineraryId.of(doc.getId()),
                TravellerId.of(doc.getTravellerId()),
                ItineraryStatus.valueOf(doc.getStatus()),
                legs,
                Money.of(doc.getAmount(), doc.getCurrency()),
                Instant.ofEpochMilli(doc.getBookedAtEpochMs()),
                Instant.ofEpochMilli(doc.getLastEventAtEpochMs()),
                score,
                lastScoredAt);
    }
}
