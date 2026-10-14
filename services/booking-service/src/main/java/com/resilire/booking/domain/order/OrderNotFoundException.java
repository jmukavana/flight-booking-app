package com.resilire.booking.domain.order;

/** Raised when an order is requested that does not exist for the Traveller. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(OrderId orderId) {
        super("Order not found: " + orderId.value());
    }
}
