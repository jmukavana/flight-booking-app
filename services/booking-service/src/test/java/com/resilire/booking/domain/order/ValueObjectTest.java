package com.resilire.booking.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Value-object invariants — the validation that keeps bad data out of the aggregate. */
class ValueObjectTest {

    @Test
    void money_normalises_scale_and_currency_case() {
        Money money = Money.of("18.9", "eur");

        assertThat(money.amount().toPlainString()).isEqualTo("18.90");
        assertThat(money.currency()).isEqualTo("EUR");
    }

    @Test
    void money_rejects_negative_amount() {
        assertThatThrownBy(() -> Money.of("-1.00", "EUR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void route_rejects_arrival_before_departure() {
        Instant departure = Instant.parse("2026-07-01T10:00:00Z");
        assertThatThrownBy(() -> new Route("LHR", "JFK", departure, departure.minusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void route_uppercases_iata_codes() {
        Instant departure = Instant.parse("2026-07-01T10:00:00Z");
        Route route = new Route("lhr", "jfk", departure, departure.plusSeconds(3600));

        assertThat(route.origin()).isEqualTo("LHR");
        assertThat(route.destination()).isEqualTo("JFK");
    }

    @Test
    void searchCriteria_rejects_same_origin_and_destination() {
        assertThatThrownBy(() -> new SearchCriteria("LHR", "lhr", LocalDate.parse("2026-07-01")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
