package com.resilire.resiliencescore.application;

import com.resilire.resiliencescore.domain.score.ItineraryFeatures;
import java.time.Instant;
import java.util.Objects;

/** Command derived from a consumed {@code OrderCreated} event — precompute a score for the booked itinerary. */
public record ScoreForOrderCommand(
        String itineraryId,
        String travellerId,
        ItineraryFeatures features,
        Instant occurredAt) {

    public ScoreForOrderCommand {
        Objects.requireNonNull(itineraryId, "itineraryId");
        Objects.requireNonNull(travellerId, "travellerId");
        Objects.requireNonNull(features, "features");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
