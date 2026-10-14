-- Database-per-service (ADR-9): logical separation locally, separate clusters in prod.
-- Only the 7 stateful services get a DB. resilience-score & itinerary use Redis;
-- ingestion, notification, api-gateway hold no relational state.
CREATE DATABASE identity;
CREATE DATABASE booking;
CREATE DATABASE recovery;
CREATE DATABASE rights;
CREATE DATABASE payments;
CREATE DATABASE partner;
CREATE DATABASE disruption;

-- Single dev role 'resilire' (created by POSTGRES_USER) owns all of them.
GRANT ALL PRIVILEGES ON DATABASE identity, booking, recovery, rights, payments, partner, disruption TO resilire;
