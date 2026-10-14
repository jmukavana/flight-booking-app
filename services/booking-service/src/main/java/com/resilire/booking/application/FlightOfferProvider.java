package com.resilire.booking.application;

import com.resilire.booking.domain.order.FlightOffer;
import com.resilire.booking.domain.order.OfferNotFoundException;
import com.resilire.booking.domain.order.Pnr;
import com.resilire.booking.domain.order.SearchCriteria;
import com.resilire.booking.domain.order.TravellerId;
import java.util.List;

/**
 * Anti-corruption port over the NDC/aggregator provider (Duffel/Amadeus, SDD §2).
 * Keeps the volatile external API behind the domain model so the provider can be
 * swapped without touching the core. Phase 1 ships an in-memory stub; a real
 * adapter implements the same contract.
 */
public interface FlightOfferProvider {

    List<FlightOffer> search(SearchCriteria criteria);

    /**
     * Re-fetch an offer by its opaque id.
     *
     * @throws OfferNotFoundException if the offer has expired or is unknown
     */
    FlightOffer findOffer(String offerId);

    /** Reserve the offer with the carrier and return the issued PNR. */
    Pnr place(FlightOffer offer, TravellerId traveller);

    /** Cancel a previously placed booking with the carrier. */
    void cancel(Pnr pnr);
}
