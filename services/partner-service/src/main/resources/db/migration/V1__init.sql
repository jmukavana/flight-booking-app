-- partner-service schema (DB: partner)
-- B2B tenant management: tenants, API keys (hash-only, driver 5), quotas,
-- whitelabel configs, and referral credits.

CREATE TABLE tenants (
    id         VARCHAR(255) PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    status     VARCHAR(50)  NOT NULL,
    plan       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_tenants_name UNIQUE (name)
);

-- API keys stored as SHA-256 hex only — raw key returned once at issuance (driver 5).
CREATE TABLE tenant_api_keys (
    id          VARCHAR(255) PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    key_hash    VARCHAR(255) NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    CONSTRAINT uq_api_keys_key_hash UNIQUE (key_hash)
);

CREATE INDEX idx_api_keys_tenant_id ON tenant_api_keys (tenant_id);

CREATE TABLE tenant_quotas (
    id                   VARCHAR(255) PRIMARY KEY,
    tenant_id            VARCHAR(255) NOT NULL,
    requests_per_minute  INT          NOT NULL,
    burst_capacity       INT          NOT NULL,
    plan                 VARCHAR(50)  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    CONSTRAINT uq_quotas_tenant_id UNIQUE (tenant_id)
);

CREATE TABLE whitelabel_configs (
    id             VARCHAR(255) PRIMARY KEY,
    tenant_id      VARCHAR(255) NOT NULL,
    logo_url       VARCHAR(1024),
    primary_color  VARCHAR(20),
    custom_domain  VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ,
    CONSTRAINT uq_whitelabel_tenant_id UNIQUE (tenant_id)
);

CREATE TABLE referrals (
    id           VARCHAR(255) PRIMARY KEY,
    referrer_id  VARCHAR(255) NOT NULL,
    referred_id  VARCHAR(255) NOT NULL,
    status       VARCHAR(50)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    credited_at  TIMESTAMPTZ
);

CREATE INDEX idx_referrals_referrer ON referrals (referrer_id);
