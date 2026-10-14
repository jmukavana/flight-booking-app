package com.resilire.itinerary.domain.itinerary;

/**
 * Phase 1 itinerary states, folded from order events. The disruption-aware states
 * (AtRisk/Disrupted/Recovering) arrive with detection + recovery in Phase 2
 * (architecture.md §5.4, roadmap.md §4).
 */
public enum ItineraryStatus {
    CONFIRMED,
    CANCELLED
}
