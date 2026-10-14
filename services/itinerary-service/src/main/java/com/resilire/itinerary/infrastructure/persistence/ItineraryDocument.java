package com.resilire.itinerary.infrastructure.persistence;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

/**
 * Redis hash for the itinerary projection. Timestamps and money are stored as
 * primitives/strings to keep the representation converter-free and stable;
 * {@code travellerId} is secondary-indexed so a Traveler's itineraries can be
 * listed without a scan.
 */
@RedisHash("itinerary")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDocument {

    @Id
    private String id;

    @Indexed
    private String travellerId;

    private String status;

    private List<LegDocument> legs;

    private String amount;

    private String currency;

    private long bookedAtEpochMs;

    private long lastEventAtEpochMs;

    // Resilience score, attached later via ScoreComputed; null until it arrives.
    private Integer scoreValue;

    private String scoreBand;

    private String scoreSource;

    private String scoreModelVersion;

    private Long scoredAtEpochMs;
}
