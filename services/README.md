# Resilire — Services

12 independent Spring Boot microservices (one Maven build each). Stack: **Java 25**, **Spring Boot 4.0.6**, group `com.resilire`, scaffolded via Spring Initializr. ML Serving is a separate **Python/FastAPI** service (ADR-7) — intentionally not here. Architecture: [`../docs/architecture.md`](../docs/architecture.md) · decision: ADR-15.

| Service | Web stack | Owns data | Role (see architecture.md §6) |
|---|---|---|---|
| `api-gateway` | Spring Cloud Gateway (reactive) | — | Edge: OIDC authn/z, routing, rate-limit |
| `identity-consent-service` | MVC | Postgres | Accounts, OAuth2 RS, immutable consent ledger |
| `booking-service` | MVC | Postgres | Search/order/cancel, emits `OrderCreated` |
| `resilience-score-service` | MVC | Redis | Score; calls ML Serving over HTTP |
| `ingestion-service` | WebFlux | — | Status/weather feed normalisation (hot path) |
| `disruption-detection-service` | WebFlux | Postgres | Authoritative disruption state machine (hot path) |
| `recovery-orchestrator-service` | MVC | Postgres | Authority-gated recovery saga |
| `rights-claims-service` | MVC | Postgres | Versioned, effective-dated eligibility + claims |
| `itinerary-service` | MVC | Redis | CQRS itinerary read model + downstream sync |
| `payments-service` | MVC | Postgres | Tokenised capture/refund/payout, ledger (PCI scope) |
| `notification-service` | MVC | — | Multi-channel alerts, priority lane |
| `partner-service` | MVC | Postgres | B2B2C tenant API, white-label, metering |

Common deps in every service: `kafka` (Spring for Apache Kafka), `actuator`, `distributed-tracing` (OTel), `lombok`. Validation/security/postgres/redis per role above.

## Run one service
```bash
cd <service> && ./mvnw spring-boot:run
```

## Build status
- **`identity-consent-service`** — built: consent ledger (append-only), OAuth2 resource server, transactional outbox (`consent.changed`).
- **`booking-service`** — built: search → order → cancel (`Order` aggregate, hexagonal), idempotent placement, transactional outbox emitting `order.created` / `order.cancelled`; NDC + Payments behind anti-corruption ports with in-memory stubs (Phase 1, no provider account needed). Resource server on the gateway-relayed JWT. Port 9002, DB `booking`.
- **`itinerary-service`** — built: CQRS read model (ADR-5) projecting `order.created` / `order.cancelled` into a Redis-backed itinerary view, and consuming `score.computed` to attach the resilience score (SDD §4). Idempotent consumer with **two independent high-water marks** (order timeline vs score timeline), tolerating at-least-once, cross-topic delivery. Traveller-scoped read API; resource server. Port 9008, Redis. `ItinerarySynced` emission + downstream auto-sync are deferred to Phase 2 (roadmap §3/§4).
- **`resilience-score-service`** — built: explainable 0–100 score (FR-2). Sync `POST /scores` point-of-sale path (cache-first, driver 7) + `order.created` consumer that precomputes and emits `score.computed`. ML Serving behind a RestClient anti-corruption port; **degrade-safe** (NFR-ACC-1): model down → last-known (flagged) → heuristic, never a fabricated model score. Redis score cache keyed by itinerary signature; resource server. Port 9003.
- **`notification-service`** — built: multi-channel alerts (FR-11). Consumes `order.created` / `order.cancelled`, applies the quiet-hours policy (URGENT bypasses it — the ADR-14 priority lane) and a channel **fallback chain** (EMAIL→PUSH→SMS), emits `notification.delivered` (the delivery log is the event stream — no DB). EMAIL/SMS/PUSH are stub adapters; default prefs (per-Traveller prefs = FR-8, Phase 2). Pure event-driven, no gateway route. Port 9010. SMTP detail is configurable (`resilire.notification.email.*`, password via `RESILIRE_MAIL_PASSWORD`).
- **`payments-service`** — built: tokenised capture/refund (FR-10). Append-only `payment_ledger` (immutable audit substrate), idempotent on `(idempotency_key, type)` so capture/refund share the booking key without double movement. Stripe behind an anti-corruption port (stub returns `ch_*`/`re_*`); PCI scope stays here (driver 6). **Transactional outbox from day one** (roadmap §7.1) emitting `payment.captured` / `payment.refunded`. Internal resource server (no gateway route). Port 9009, DB `payments`. **Wired:** `booking-service` calls `POST /payments/captures` and `/refunds` over HTTP, relaying the traveller JWT.
- **`ingestion-service`** — built: status/weather feed normalisation (FR-3 hot path). Reactive `POST /feeds/flight-status` and `/feeds/weather-alerts` endpoints normalise raw carrier strings into typed domain events (`FlightStatusCode`, `AlertSeverity`), compute delay minutes, and publish to Kafka. Topics: `flight.status.updated` (keyed by flightId) · `weather.alert.published` (keyed by alertId). No persistence — stateless normaliser. Port 9004.
- **`disruption-detection-service`** — built: authoritative disruption state machine (FR-3). Consumes `flight.status.updated` → runs the detection state machine: early-warning threshold 45 min, SEVERE ≥ 120 min, cancellation/diversion → instant detection. One ACTIVE disruption per flight; subsequent updates adjust severity without creating duplicates. Landing resolves the episode. Transactional outbox emitting `disruption.detected` / `disruption.resolved`. `GET /disruptions/active` for recovery-orchestrator queries. Port 9005, DB `disruption`.
- **`api-gateway`** — built: single front door (port 8080). OIDC client (authorization-code + PKCE) against Keycloak at `http://localhost:8085/realms/resilire`. `TokenRelay=` default filter forwards the access token downstream as Bearer so all 7 routed services validate it as resource servers. `RequestRateLimiter` default filter: 20 req/s per user, burst 30, Redis token bucket (`principalKeyResolver` → `preferred_username`). RP-initiated logout ends the Keycloak session. Routes: `/api/identity/**` → 9001 · `/api/booking/**` → 9002 · `/api/score/**` → 9003 · `/api/recovery/**` → 9006 · `/api/rights/**` → 9007 · `/api/itinerary/**` → 9008 · `/api/partner/**` → 9011.
- The remaining 3 services (`recovery-orchestrator-service`, `rights-claims-service`, `partner-service`) are Initializr scaffolds — domain code is the next step (Phase 2).

## Event flow wired so far
```
booking ──order.created──┬──▶ itinerary       (projects read model)
                         ├──▶ resilience-score (precomputes score)
                         │          └──score.computed──▶ itinerary (attaches score)
                         └──▶ notification    (sends confirmation, emits notification.delivered)
booking ──order.cancelled──┬──▶ itinerary     (cancels projection)
                           └──▶ notification   (sends cancellation)
booking ──────────────────────▶ payments      (HTTP: capture on place, refund on cancel)

[external feed] ──▶ ingestion (flight.status.updated / weather.alert.published)
                          └──▶ disruption-detection ──disruption.detected──▶ (notification Phase 2)
                                                    ──disruption.resolved──▶ (recovery-orchestrator Phase 2)
```
Closed loops: **book → itinerary projected → scored → score visible on the itinerary** · **feed → normalised → disruption detected → authoritative state persisted**.

## Not yet wired (deliberate — Phase 1 scope, see roadmap.md §3)
Schema registry · real NDC/Stripe/ML adapters (all stubbed locally) · disruption → notification alert wire (Phase 2: needs itinerary↔flight correlation) · `ItinerarySynced` + downstream auto-sync · OAuth2 issuer config for the scaffolds. The built services run against the local infra in [`../infra`](../infra).
