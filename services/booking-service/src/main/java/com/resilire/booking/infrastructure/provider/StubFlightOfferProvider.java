package com.resilire.booking.infrastructure.provider;

import com.resilire.booking.application.FlightOfferProvider;
import com.resilire.booking.domain.order.FlightOffer;
import com.resilire.booking.domain.order.Money;
import com.resilire.booking.domain.order.OfferNotFoundException;
import com.resilire.booking.domain.order.Pnr;
import com.resilire.booking.domain.order.Route;
import com.resilire.booking.domain.order.SearchCriteria;
import com.resilire.booking.domain.order.TravellerId;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for the NDC/aggregator adapter (SDD §2/§3). It satisfies the
 * {@link FlightOfferProvider} contract so the rest of the service is real and
 * runnable without a Duffel/Amadeus account; a production adapter drops in behind
 * the same port. Searched offers are cached by id so a later {@code findOffer} can
 * resolve the Traveller's selection.
 */
@Component
public class StubFlightOfferProvider implements FlightOfferProvider {

    private static final Logger log = LoggerFactory.getLogger(StubFlightOfferProvider.class);
    private static final String PROVIDER = "STUB-AIR";
    private static final LocalTime[] DEPARTURE_SLOTS = {LocalTime.of(7, 30), LocalTime.of(13, 15), LocalTime.of(19, 0)};
    private static final long[] PRICES = {18900, 24500, 31200};

    private final Map<String, FlightOffer> offers = new ConcurrentHashMap<>();

    @Override
    public List<FlightOffer> search(SearchCriteria criteria) {
        List<FlightOffer> generated = new java.util.ArrayList<>(DEPARTURE_SLOTS.length);
        for (int i = 0; i < DEPARTURE_SLOTS.length; i++) {
            Instant departure = criteria.departureDate().atTime(DEPARTURE_SLOTS[i]).toInstant(ZoneOffset.UTC);
            Instant arrival = departure.plus(Duration.ofHours(2).plusMinutes(20));
            Route route = new Route(criteria.origin(), criteria.destination(), departure, arrival);
            Money price = new Money(BigDecimal.valueOf(PRICES[i], 2), "EUR");
            FlightOffer offer = new FlightOffer(UUID.randomUUID().toString(), PROVIDER, route, price);
            offers.put(offer.offerId(), offer);
            generated.add(offer);
        }
        return List.copyOf(generated);
    }

    @Override
    public FlightOffer findOffer(String offerId) {
        FlightOffer offer = offers.get(offerId);
        if (offer == null) {
            throw new OfferNotFoundException(offerId);
        }
        return offer;
    }

    @Override
    public Pnr place(FlightOffer offer, TravellerId traveller) {
        Pnr pnr = Pnr.of(randomLocator());
        log.info("Stub provider issued PNR {} for offer {} ({} {}→{})",
                pnr.value(), offer.offerId(), offer.provider(),
                offer.route().origin(), offer.route().destination());
        return pnr;
    }

    @Override
    public void cancel(Pnr pnr) {
        log.info("Stub provider cancelled PNR {}", pnr.value());
    }

    private static String randomLocator() {
        // A 6-char alphanumeric locator, the shape carriers actually issue.
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
