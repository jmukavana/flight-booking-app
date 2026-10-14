# Infra

Two layers, deliberately separated by maturity:

| Path | What | Status |
|---|---|---|
| `docker-compose.yml` + `postgres/ kafka/ keycloak/` | Local-dev backing services | **Working** — run it now |
| `terraform/` | Production AWS/EKS estate | **Skeleton** — built in Phase 1 (see its README) |

## Local stack — bring it up

```bash
cd infra
docker compose up -d
docker compose ps           # all healthy?
docker compose logs -f kafka-init   # confirms topics created, then exits
```

| Service | Host endpoint | Credentials / notes |
|---|---|---|
| PostgreSQL | `localhost:5433` | `resilire` / `resilire`; 7 DBs (one per stateful service, ADR-9). Host port 5433 to avoid clashing with a host Postgres on 5432 |
| Redis | `localhost:6379` | no auth (local only) |
| Kafka | `localhost:9092` | bootstrap; in-container use `kafka:29092` |
| Schema Registry (Apicurio) | `localhost:8081` | API `/apis/registry/v2` |
| Keycloak (OIDC) | `localhost:8085` | admin `admin`/`admin`; realm `resilire` |
| Kafka UI | `localhost:8088` | dev convenience |

OIDC issuer for the gateway & resource servers:
`http://localhost:8085/realms/resilire` — clients `resilire-gateway` (confidential, secret `resilire-gateway-secret`) and `resilire-app` (public + PKCE). Test users: `traveller1`/`password`, `ops1`/`password`.

Kafka topics created by `kafka-init` (architecture.md §7): the 12 domain-event topics + `.DLT` for the audit-bearing streams (`disruption.detected`, `recovery.rebooked`, `rights.eligibility.determined`, `payment.settled`).

## How services connect (not yet wired in the apps)

Service hostnames on the `resilire-net` network *are* the discovery mechanism locally — the Docker-DNS analogue of prod Kubernetes-DNS (no Eureka, ADR-1). The 12 services still have empty `application.properties`; wiring datasource / Kafka bootstrap / OIDC issuer into each service is the **next step** (Phase 1 foundation), not part of this infra layer.

Suggested per-service config (when wiring):
- JPA: `jdbc:postgresql://localhost:5433/<db>` (`booking`, `identity`, `rights`, `payments`, `partner`, `recovery`, `disruption`)
- Kafka: `spring.kafka.bootstrap-servers=localhost:9092`
- Resource server: `spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8085/realms/resilire`

## Image tags

Pinned to sensible stable versions. If a tag 404s on first pull, bump it — these were chosen, not pull-tested in this environment. Dev-only tools (`kafka-ui`) float on `latest` intentionally.

## Reset

```bash
docker compose down -v        # wipes pgdata + kafkadata volumes
```
