package com.resilire.itinerary.domain.itinerary;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port owned by the domain, implemented by a Redis adapter. {@code save}
 * is an upsert keyed by the itinerary id — the projection is overwritten, not
 * appended.
 */
public interface ItineraryRepository {

    void save(Itinerary itinerary);

    Optional<Itinerary> findById(ItineraryId id);

    List<Itinerary> findByTraveller(TravellerId traveller);
}
