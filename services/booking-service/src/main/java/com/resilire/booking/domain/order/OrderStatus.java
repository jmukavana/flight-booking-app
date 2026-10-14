package com.resilire.booking.domain.order;

/**
 * Lifecycle of an order in Phase 1. Placement is synchronous (provider booked +
 * payment captured before the aggregate exists), so an order is born
 * {@code CONFIRMED}; the async/{@code PENDING} capture path is a later evolution
 * (roadmap.md §3).
 */
public enum OrderStatus {
    CONFIRMED,
    CANCELLED
}
