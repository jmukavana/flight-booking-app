-- recovery-orchestrator-service schema (DB: recovery)
-- One recovery case per disruption (disruption_id unique); options are the
-- set of traveller choices proposed by the orchestrator.

CREATE TABLE recovery_cases (
    id               VARCHAR(255) PRIMARY KEY,
    disruption_id    VARCHAR(255) NOT NULL,
    flight_id        VARCHAR(255) NOT NULL,
    origin           VARCHAR(3)   NOT NULL,
    destination      VARCHAR(3)   NOT NULL,
    disruption_cause VARCHAR(100) NOT NULL,
    severity         VARCHAR(50)  NOT NULL,
    delay_minutes    INT          NOT NULL DEFAULT 0,
    status           VARCHAR(50)  NOT NULL,
    auto_approved    BOOLEAN      NOT NULL DEFAULT FALSE,
    opened_at        TIMESTAMPTZ  NOT NULL,
    resolved_at      TIMESTAMPTZ,
    CONSTRAINT uq_recovery_disruption_id UNIQUE (disruption_id)
);

CREATE TABLE recovery_options (
    id          VARCHAR(255) PRIMARY KEY,
    case_id     VARCHAR(255) NOT NULL,
    option_type VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_options_case ON recovery_options (case_id);

-- Transactional outbox (ADR-10).
CREATE TABLE recovery_outbox (
    id           VARCHAR(255) PRIMARY KEY,
    topic        VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_recovery_outbox_unpublished ON recovery_outbox (occurred_at) WHERE published_at IS NULL;
