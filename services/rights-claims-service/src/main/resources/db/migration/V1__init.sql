-- rights-claims-service schema (DB: rights)
-- EU261/EC261 and other jurisdiction eligibility engine: rulesets, rules,
-- assessments, and traveller claims. Lifecycle: DRAFT → ACTIVE → SUPERSEDED.

CREATE TABLE eligibility_rulesets (
    id             VARCHAR(255) PRIMARY KEY,
    jurisdiction   VARCHAR(50)  NOT NULL,
    version        VARCHAR(50)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    effective_from DATE         NOT NULL,
    effective_to   DATE,
    created_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_ruleset_jurisdiction_version UNIQUE (jurisdiction, version)
);

CREATE INDEX idx_rulesets_jurisdiction_status ON eligibility_rulesets (jurisdiction, status);

-- Individual rules within a ruleset. Null bounds = uncapped.
CREATE TABLE eligibility_rules (
    id                    VARCHAR(255)   PRIMARY KEY,
    ruleset_id            VARCHAR(255)   NOT NULL,
    entitlement_type      VARCHAR(50)    NOT NULL,
    cause                 VARCHAR(50),
    delay_min_minutes     INT            NOT NULL DEFAULT 0,
    delay_max_minutes     INT,
    flight_distance_km_min INT,
    flight_distance_km_max INT,
    compensation_amount   NUMERIC(10, 2),
    currency              VARCHAR(3),
    description           VARCHAR(512)
);

CREATE INDEX idx_rules_ruleset ON eligibility_rules (ruleset_id);

CREATE TABLE eligibility_assessments (
    id                  VARCHAR(255)   PRIMARY KEY,
    disruption_id       VARCHAR(255),
    flight_id           VARCHAR(255)   NOT NULL,
    origin              VARCHAR(3)     NOT NULL,
    destination         VARCHAR(3)     NOT NULL,
    jurisdiction        VARCHAR(50)    NOT NULL,
    delay_minutes       INT            NOT NULL,
    cause               VARCHAR(50)    NOT NULL,
    flight_distance_km  INT,
    ruleset_id          VARCHAR(255),
    ruleset_version     VARCHAR(50),
    eligible            BOOLEAN        NOT NULL,
    entitlement_type    VARCHAR(50),
    compensation_amount NUMERIC(10, 2),
    currency            VARCHAR(3),
    reasoning           VARCHAR(1024),
    assessed_at         TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_assessment_disruption ON eligibility_assessments (disruption_id);
CREATE INDEX idx_assessment_flight     ON eligibility_assessments (flight_id);

CREATE TABLE claims (
    id            VARCHAR(255)  PRIMARY KEY,
    assessment_id VARCHAR(255)  NOT NULL,
    traveller_id  VARCHAR(255)  NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    submitted_at  TIMESTAMPTZ   NOT NULL,
    resolved_at   TIMESTAMPTZ,
    payment_ref   VARCHAR(255),
    notes         VARCHAR(1024)
);

CREATE INDEX idx_claim_traveller  ON claims (traveller_id);
CREATE INDEX idx_claim_assessment ON claims (assessment_id);

-- Transactional outbox (ADR-10).
CREATE TABLE rights_outbox (
    id           VARCHAR(255) PRIMARY KEY,
    topic        VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_rights_outbox_unpublished ON rights_outbox (occurred_at) WHERE published_at IS NULL;
