-- disruption-detection-service schema (DB: disruption)
-- One active disruption episode per flight at a time. ACTIVE → RESOLVED;
-- a new disruption of the same flight starts a new row.

CREATE TABLE disruptions (
    id              VARCHAR(255) PRIMARY KEY,
    flight_id       VARCHAR(255) NOT NULL,
    origin          VARCHAR(3)   NOT NULL,
    destination     VARCHAR(3)   NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    cause           VARCHAR(50)  NOT NULL,
    severity        VARCHAR(20)  NOT NULL,
    delay_minutes   INT          NOT NULL DEFAULT 0,
    detected_at     TIMESTAMPTZ  NOT NULL,
    resolved_at     TIMESTAMPTZ,
    last_updated_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_disruption_flight_status ON disruptions (flight_id, status);

-- Transactional outbox (ADR-10).
CREATE TABLE disruption_outbox (
    id           VARCHAR(255) PRIMARY KEY,
    topic        VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_disruption_outbox_unpublished ON disruption_outbox (occurred_at) WHERE published_at IS NULL;
