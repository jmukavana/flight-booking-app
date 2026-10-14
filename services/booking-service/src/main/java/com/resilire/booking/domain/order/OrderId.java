package com.resilire.booking.domain.order;

import java.util.Objects;
import java.util.UUID;

/** Identity of an {@link Order}. Value object. */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "orderId value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }
}
