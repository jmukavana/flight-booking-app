package com.resilire.itinerary.application;

import com.resilire.itinerary.domain.itinerary.Itinerary;
import com.resilire.itinerary.domain.itinerary.ItineraryId;
import com.resilire.itinerary.domain.itinerary.ItineraryNotFoundException;
import com.resilire.itinerary.domain.itinerary.ItineraryRepository;
import com.resilire.itinerary.domain.itinerary.TravellerId;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Read side of the CQRS itinerary model. Reads are served straight from the Redis
 * projection, so they stay available even when the upstream order path is down
 * (NFR-AVL-1). Every read is scoped to the authenticated Traveller.
 */
@Service
public class ItineraryQueryService {

    private final ItineraryRepository itineraries;

    public ItineraryQueryService(ItineraryRepository itineraries) {
        this.itineraries = itineraries;
    }

    public Itinerary getItinerary(TravellerId traveller, ItineraryId id) {
        return itineraries.findById(id)
                .filter(itinerary -> itinerary.traveller().equals(traveller))
                .orElseThrow(() -> new ItineraryNotFoundException(id));
    }

    public List<Itinerary> listForTraveller(TravellerId traveller) {
        return itineraries.findByTraveller(traveller);
    }
}
