package com.resilire.booking.domain.order;

import java.util.Objects;

/**
 * A bookable offer as returned by a provider through the anti-corruption layer
 * (SDD §2). The {@code offerId} is opaque and provider-scoped; the rest is the
 * normalised domain view the rest of the service reasons about. Value object.
 */
public record FlightOffer(String offerId, String provider, Route route, Money price) {

    public FlightOffer {
        Objects.requireNonNull(offerId, "offerId");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(price, "price");
        if (offerId.isBlank()) {
            throw new IllegalArgumentException("offerId must not be blank");
        }
        if (provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
    }
}
