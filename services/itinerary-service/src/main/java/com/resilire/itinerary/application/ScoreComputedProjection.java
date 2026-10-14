package com.resilire.itinerary.application;

import com.resilire.itinerary.domain.itinerary.ItineraryId;
import com.resilire.itinerary.domain.itinerary.ResilienceScore;
import java.util.Objects;

/** Application command derived from a consumed {@code ScoreComputed} integration event. */
public record ScoreComputedProjection(ItineraryId itineraryId, ResilienceScore score) {

    public ScoreComputedProjection {
        Objects.requireNonNull(itineraryId, "itineraryId");
        Objects.requireNonNull(score, "score");
    }
}
