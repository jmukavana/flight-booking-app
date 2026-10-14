# System Design Document (SDD)

**Product:** Disruption-Resilient Flight Booking Platform (*Resilire*)
**Document:** SDD v1.0 — IEEE 1016 aligned
**Status:** Baseline architecture for the SRS v1.0
**Last updated:** 2026-05-17
**Companion docs:** [`SRS.md`](./SRS.md) · [`aviation-problem-definition.md`](./aviation-problem-definition.md) · [`SDLC-plan-and-test.md`](./SDLC-plan-and-test.md)

---

## 1. Design goals & drivers

Derived from SRS NFRs and dossier §11 constraints:

1. **Event-driven core** — disruption is an event-propagation problem (primary→reactionary); the architecture must model state changes as events, not request/response polling.
2. **Externalised, versioned rules & models** — rights regimes are mid-reform; prediction models evolve. Rules/models are *data with versions and effective dates*, deployable without code release (NFR-MNT-1, NFR-ACC-1).
3. **Graceful degradation of authority** — assisted-with-consent today, automated where partnership/authority exists; the same code path, gated by policy (dossier §11).
4. **Idempotency everywhere** — money and bookings; exactly-once *effect* (NFR-REL-1).
5. **Tenant isolation & data residency** — B2B2C multi-tenancy; EU PII in EU (NFR-PRIV-1).
6. **PCI scope minimisation** — card data never enters core services.

---

## 2. Architectural overview

Style: **microservices + event streaming**, fronted by an API gateway, with a CQRS-flavoured read model for itinerary state.

```
                          ┌───────────────────────────────────────────┐
   Mobile (Flutter)/Web ─▶│  API Gateway  (authn/z, rate-limit, audit) │
   Partner API/SSO  ────▶ └───────────────────────┬───────────────────┘
                                                   │ REST / gRPC
        ┌───────────────┬──────────────┬───────────┴───────┬───────────────┬───────────────┐
        ▼               ▼              ▼                    ▼               ▼               ▼
  Booking Svc     Resilience      Disruption           Recovery        Rights/Claims   Itinerary Svc
  (search/order)  Score Svc       Detection Svc        Orchestrator    Engine          (read model)
        │               │              │                    │               │               │
        └───────────────┴───────┬──────┴──────────┬─────────┴───────┬───────┴───────────────┘
                                 ▼                 ▼                 ▼
                        ┌──────────────────  EVENT BUS (Kafka)  ──────────────────┐
                        │ FlightStatusChanged · DisruptionDetected · Rebooked ·    │
                        │ EligibilityDetermined · PayoutInitiated · ItinerarySynced│
                        └───────────────────────────────────────────────────────────┘
        ▲                 ▲                  ▲                ▲                 ▲
        │                 │                  │                │                 │
  NDC/Aggregator   Status/Weather/      Payments         Comms (SMS/      ML Serving
  Adapter          Airport Ingest       (tokenised)      push/email)      (FastAPI)

  Data: PostgreSQL (transactional) · TimescaleDB (status/OTP telemetry) · Redis (cache/locks/queues) · S3 (docs/claims)
```

### 2.1 Why this shape
- **Event bus** decouples detection from recovery and lets reactionary-delay propagation be modelled as event cascades.
- **Read model (Itinerary Svc)** gives offline-tolerant, low-latency itinerary views independent of upstream availability (NFR-AVL-1 degraded mode).
- **Adapters** isolate volatile external APIs (NDC, status) behind anti-corruption layers — swap Duffel→direct NDC without touching core (dossier §11 / SRS constraints).

---

## 3. Component design

| Component | Responsibility | Key SRS refs | Notes |
|---|---|---|---|
| **API Gateway** | AuthN/Z (OAuth2/OIDC, partner SSO), rate-limit, request audit | FR-9, FR-12, NFR-SEC-1 | Spring Cloud Gateway + Spring Security (resource server / OIDC); managed IdP optional for partner federation |
| **Booking Service** | Search, fare/ancillary, order/PNR, cancel/refund | FR-1, FR-10 | Stateless; calls NDC Adapter; emits `OrderCreated` |
| **NDC/Aggregator Adapter** | Anti-corruption layer over Duffel/Amadeus → domain model | FR-1, FR-4 | Pluggable provider strategy |
| **Resilience Score Service** | Compose features, call ML serving, return explainable score | FR-2, NFR-ACC-1, NFR-PERF-1 | Caches by itinerary signature in Redis |
| **Ingestion Service** | Normalise flight-status/weather/airport feeds → `FlightStatusChanged` | FR-3 | Writes telemetry to TimescaleDB |
| **Disruption Detection Service** | Correlate events to active itineraries; single authoritative disruption state; emit `DisruptionDetected` | FR-3, NFR-REL-1 | Dedup + state machine per itinerary |
| **Recovery Orchestrator** | Generate ranked options; assisted/automated execution gated by consent/authority policy; idempotent | FR-4, FR-8 | Saga pattern; compensating actions |
| **Rights & Claims Engine** | Versioned multi-jurisdiction eligibility; claim generation/submission/tracking | FR-5, FR-7, NFR-COMP-1 | Rules as versioned data (see §5) |
| **Itinerary Service** | Unified itinerary read model; import/parse; downstream sync | FR-6, NFR-AVL-1 | CQRS projection from events |
| **Payments Service** | Capture/refund/payout via tokenised processor; ledger | FR-10, NFR-SEC-2 | Out of PCI scope for other services |
| **Notification Service** | Multi-channel, fallback, prefs/quiet-hours, delivery tracking | FR-11 | Priority lane for disruption alerts |
| **Partner Service** | Tenant config, white-label dashboard data, metering | FR-9 | Strict tenant isolation |
| **ML Serving (FastAPI)** | Misconnection/cancellation prediction; recovery ranking | FR-2, FR-4, NFR-ACC-1 | Model registry + versioned endpoints |
| **Identity & Consent** | Accounts, MFA, RBAC, GDPR consent ledger | FR-12, NFR-PRIV-1 | Immutable consent log |

---

## 4. Data design (core entities)

Relational (PostgreSQL) unless noted.

- **User**(id, contact, locale, MFA, role) — GDPR-scoped; region-pinned.
- **Consent**(id, user_id, scope, granted_at, revoked_at, version) — append-only.
- **Itinerary**(id, user_id, status, ticket_structure[single|separate], resilience_score, score_model_version) — aggregate root.
- **Segment**(id, itinerary_id, marketing_carrier, operating_carrier, dep/arr, MCT_context, status).
- **Booking/Order**(id, itinerary_id, provider, PNR, fare_rules, ancillaries, amount, currency).
- **DownstreamBooking**(id, itinerary_id, type[stay|ground|insurance], provider_ref, sync_state).
- **DisruptionEvent**(id, itinerary_id, type, cause[controllable|uncontrollable], detected_at, source, state).
- **RecoveryAction**(id, disruption_id, options_json, chosen, mode[assisted|auto], authority_basis, idempotency_key, audit).
- **RightsRuleset**(id, jurisdiction, version, effective_from, effective_to, rules_json) — versioned, never mutated in place.
- **EligibilityDecision**(id, disruption_id, ruleset_version, outcome, explanation, decided_at) — immutable.
- **Claim**(id, eligibility_id, status, submitted_at, payout_id, fee_disclosed).
- **PaymentLedger**(id, type[capture|refund|payout], amount, currency, processor_ref, idempotency_key).
- **Telemetry** (TimescaleDB): flight-status & OTP time-series feeding the score model.
- **PartnerTenant**(id, name, residency_region, api_tier, branding).

Integrity rules: monetary writes require `idempotency_key`; `EligibilityDecision` and `Consent` are append-only; EU `User`/PII rows physically reside in the EU cluster.

---

## 5. Key design mechanisms

### 5.1 Versioned, effective-date rights engine (FR-5)
Rules are JSON rulesets keyed by `(jurisdiction, version, effective_from/to)`. Evaluation selects the ruleset whose effective window contains the *flight date*, never "latest". This makes the EU261 reform (Council 4h/6h vs Parliament 3h, in trilogue — dossier §8) a data change with staged rollout, satisfying NFR-MNT-1. Each decision stores the exact ruleset version for audit (NFR-COMP-1).

### 5.2 Authority-gated recovery (FR-4/FR-8)
Recovery Orchestrator runs the same option-generation logic always; an **authority policy** decides the terminal step: `assisted` (default, requires user confirmation), `auto` (requires valid Auto-Recovery consent within scope+cap), or `handoff` (no programmatic execution path). New partner agreements widen the policy, not the code (dossier §11).

### 5.3 Idempotency & sagas
Booking, rebooking, refund, payout are sagas with `idempotency_key` and compensating actions; retried safely (NFR-REL-1) to guarantee exactly-once *effect* (no double-booking/double-payout).

### 5.4 Disruption state machine
Per itinerary: `OK → AtRisk → Disrupted → Recovering → Resolved`. Detection Service is the single writer; duplicate/conflicting source signals collapse into one authoritative transition (FR-3).

---

## 6. Sequence flows (abridged)

**Booking with Resilience Score**
`Client → Gateway → Booking(search) → NDC Adapter → Resilience Score(ML serving) → Client(displays score) → Booking(order) → Payments(tokenised) → emit OrderCreated → Itinerary projection`

**Disruption recovery**
`Ingestion(FlightStatusChanged) → Detection(correlate, DisruptionDetected) → Notification(≤2 min alert) → user taps Assist → Recovery Orchestrator(ranked options) → [authority policy] → execute via NDC Adapter → Rebooked → Itinerary sync → Rights Engine(eligibility) → Claim`

**Automatic refund (US DOT)**
`DisruptionDetected(cancellation,no-rebook,US) → Rights Engine(automatic-refund mandate, ruleset vX) → Payments(refund original tender within deadline) → ledger + immutable EligibilityDecision`

---

## 7. Technology stack (recommended baseline)

Consistent with prior project direction; portable (NFR-PORT-1), substitutable.

| Layer | Choice | Rationale |
|---|---|---|
| Mobile | Flutter (iOS/Android, single codebase) | One UI codebase; native performance; offline-tolerant itinerary view |
| Web | Next.js + Tailwind | SSR for marketing/SEO; partner white-label dashboard |
| API Gateway / IdP | Spring Cloud Gateway + Spring Security (OAuth2/OIDC) | Authz, rate-limit, partner SSO, audit — same JVM/observability stack as services |
| Core services | Spring Boot (Java 25 LTS) on virtual threads; WebFlux on hot detection/recovery paths | JVM maturity for a txn/compliance domain; virtual threads keep blocking NDC/payment calls cheap; reactive only where throughput demands |
| Eventing | Apache Kafka via Spring for Apache Kafka / Spring Cloud Stream | Ordered, replayable disruption pipelines; binder abstraction keeps Kafka substitutable |
| Persistence/sagas | Spring Data (JPA + JDBC) · transactional-outbox · saga orchestration (Axon or hand-rolled) for recovery/payout | Exactly-once effects under retries (NFR-REL-1); outbox avoids dual-write |
| ML | Python + FastAPI, gradient-boosted models, model registry/MLflow | Prediction + recovery ranking, versioned serving — language-independent over HTTP/gRPC, unchanged |
| Data | PostgreSQL (txn) · TimescaleDB (telemetry) · Redis (cache/locks, via Spring Data Redis) · S3 (docs) | Integrity + time-series + low-latency state |
| Infra | AWS EKS, IaC (Terraform), GitHub Actions, OpenTelemetry + Datadog/Sentry | Scalable, observable, multi-region |
| Payments | Stripe (+ secondary), PCI tokenisation | Multi-currency, isolated PCI scope |
| Comms | SMS/WhatsApp, email, FCM/APNs | Multi-channel with fallback |

UI must follow the `frontend-design-taste` standard (no generic AI-default look; designed empty/error/loading states; WCAG 2.2 AA per NFR-A11Y-1).

---

## 8. Cross-cutting concerns

- **Security:** OWASP ASVS L2; mTLS service-to-service; secrets in managed vault; least-privilege IAM; PCI scope confined to Payments + processor.
- **Privacy/residency:** region-pinned EU/NA clusters; data-subject export/erasure workflow; immutable consent ledger.
- **Observability:** distributed tracing across the event bus; per-itinerary correlation id; SLO dashboards; MTTD ≤5 min.
- **Resilience:** circuit breakers on every external adapter; degraded mode (cached itinerary view) when upstreams fail; Kafka replay for recovery.
- **Compliance audit:** append-only logs for rights decisions, recovery actions, payouts, consent.

---

## 9. Architecture Decision Records (ADR summary)

| ADR | Decision | Rationale | Trade-off accepted |
|---|---|---|---|
| ADR-1 | Event-driven core (Kafka) | Disruption = event propagation incl. reactionary cascade | Higher ops complexity vs request/response |
| ADR-2 | Rights as versioned data, not code | EU261 reform in trilogue; multi-jurisdiction churn | Need a rules-authoring + governance process |
| ADR-3 | Adapter/anti-corruption over NDC | Inventory access ≠ API access; provider will change | Extra mapping layer |
| ADR-4 | Authority-gated single recovery path | Assisted→automated is a policy, not a rewrite | Policy engine must be airtight |
| ADR-5 | CQRS itinerary read model | Offline tolerance + availability under upstream failure | Eventual consistency in itinerary view |
| ADR-6 | Region-pinned PII | GDPR data residency | Cross-region ops tooling cost |
| ADR-7 | Spring Boot / Java 25 LTS (virtual threads) — single backend platform; ML stays Python | Mature txn/messaging/security stack for a payments + rights domain; virtual threads retire the separate Go tier | Higher per-service footprint than Go/Node; WebFlux only on hot event paths |
| ADR-8 | Flutter for mobile (single iOS/Android codebase), Next.js for web | One mobile codebase for a small team; native-grade disruption-alert UX; web kept separate for SEO/white-label | Two frontend toolchains (Dart + TS); shared logic must be duplicated or pushed server-side |

---

## 10. Open design questions (tracked in plan §risk)

1. Pre-partnership rebooking execution path (handoff vs limited NDC) — depends on B2B pilot (dossier §13).
2. Separate-ticket protection: parametric insurance vs best-effort rebooking — unit economics unresolved (FR-7).
3. ML cold-start: bootstrap accuracy on open OTP data before partner inventory improves it (NFR-ACC-1).
