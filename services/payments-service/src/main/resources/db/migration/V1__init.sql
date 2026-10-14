-- payments-service schema (DB: payments)
-- Immutable payment ledger (SDD §4): capture, refund, and payout movements.
-- (idempotency_key, type) unique constraint prevents double-captures/refunds
-- while allowing the same key to appear once per movement type.

CREATE TABLE payment_ledger (
    id              UUID         PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL,
    traveller_id    VARCHAR(255) NOT NULL,
    amount          NUMERIC      NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    processor_ref   VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    related_ref     VARCHAR(255),
    recorded_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_ledger_key_type UNIQUE (idempotency_key, type)
);

CREATE INDEX idx_ledger_traveller ON payment_ledger (traveller_id);

-- Transactional outbox (ADR-10).
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
