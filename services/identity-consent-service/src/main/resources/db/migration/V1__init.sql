-- identity-consent-service schema (DB: identity)
-- Append-only consent ledger: grant/revoke never update rows, they insert new ones.
-- (user_id, version) unique constraint makes concurrent double-appends a violation.

CREATE TABLE consent_entry (
    id          UUID         PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    scope       VARCHAR(100) NOT NULL,
    decision    VARCHAR(20)  NOT NULL,
    version     BIGINT       NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_consent_user_version UNIQUE (user_id, version)
);

CREATE INDEX idx_consent_user ON consent_entry (user_id);

-- Transactional outbox (no topic column — single-topic service, relay knows the topic).
CREATE TABLE outbox (
    id             UUID         PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        TEXT         NOT NULL,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox (occurred_at) WHERE published_at IS NULL;
