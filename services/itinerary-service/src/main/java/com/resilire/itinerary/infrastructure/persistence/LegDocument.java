package com.resilire.itinerary.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Embedded leg of an {@link ItineraryDocument}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegDocument {

    private String origin;

    private String destination;

    private long departureEpochMs;

    private long arrivalEpochMs;

    private String marketingCarrier;

    private String pnr;
}
