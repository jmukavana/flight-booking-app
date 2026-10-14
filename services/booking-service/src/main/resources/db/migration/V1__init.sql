-- booking-service schema (DB: booking)
-- One order per confirmed booking; idempotency_key unique constraint turns a
-- racing duplicate placement into a constraint violation (SDD §5.3).

CREATE TABLE orders (
    id               UUID         PRIMARY KEY,
    itinerary_id     UUID         NOT NULL,
    traveller_id     VARCHAR(255) NOT NULL,
    provider         VARCHAR(255) NOT NULL,
    pnr              VARCHAR(255) NOT NULL,
    origin           VARCHAR(3)   NOT NULL,
    destination      VARCHAR(3)   NOT NULL,
    departure        TIMESTAMPTZ  NOT NULL,
    arrival          TIMESTAMPTZ  NOT NULL,
    amount           NUMERIC      NOT NULL,
    currency         VARCHAR(3)   NOT NULL,
    payment_ref      VARCHAR(255) NOT NULL,
    idempotency_key  VARCHAR(255) NOT NULL,
    status           VARCHAR(50)  NOT NULL,
    placed_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key)
);

-- Transactional outbox (ADR-10): written in same transaction as the order;
-- relay publishes to Kafka and stamps published_at.
CREATE TABLE outbox (
    id             UUID         PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    topic          VARCHAR(255) NOT NULL,
    payload        TEXT         NOT NULL,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox (occurred_at) WHERE published_at IS NULL;
