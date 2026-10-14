package com.resilire.itinerary.domain.itinerary;

/** Raised when an itinerary is requested that does not exist for the Traveller. */
public class ItineraryNotFoundException extends RuntimeException {

    public ItineraryNotFoundException(ItineraryId id) {
        super("Itinerary not found: " + id.value());
    }
}
