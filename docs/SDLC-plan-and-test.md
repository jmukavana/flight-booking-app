# Project & Quality Management Plan

**Product:** Disruption-Resilient Flight Booking Platform (*Resilire*)
**Document:** SDLC Plan, Risk Register, Test Plan & Requirements Traceability Matrix — v1.0
**Status:** Baseline
**Last updated:** 2026-05-17
**Companion docs:** [`SRS.md`](./SRS.md) · [`SDD.md`](./SDD.md) · [`aviation-problem-definition.md`](./aviation-problem-definition.md)

---

## 1. Lifecycle model

**Incremental / Agile with regulatory & partnership gates.** Pure waterfall is wrong here (rules and partner authority change mid-build); pure unbounded agile is wrong too (payments, PCI, GDPR, passenger-rights demand hard compliance gates). Model: 2-week sprints inside phased increments (MVP → V1 → V2 → Scale), each increment exiting through a **gate** (security/privacy review + compliance sign-off + accuracy threshold + partner readiness).

| Phase | Scope (from dossier roadmap) | Exit gate |
|---|---|---|
| **MVP (M0–6)** | FR-1,2,3,6(hub),10,11,12 — search/book, Resilience Score v1, disruption detect+alert, unified itinerary, payments, accounts | Score model ≥ target AUC on open data; PCI/GDPR review passed; 1 B2B LOI |
| **V1 (M7–12)** | FR-4(auto opt-in),5,6(sync),8,9 — assisted→automated recovery, rights/claims engine, partner API/dashboard | Legal opinion on rebooking/claims authority; 1 carrier + 1 OTA/TMC pilot live |
| **V2 (M13–18)** | FR-7 parametric; AI forecasting depth; multi-modal recovery; jurisdiction expansion | Multi-jurisdiction rules audited; parametric unit economics validated |
| **Scale (M18–24)** | Partner network, white-label scale, ancillary marketplace | Series-A operational metrics met |

---

## 2. Work breakdown structure (top level)

1. **Discovery & validation** — WTP survey (≥500), interviews, pilot-partner LOIs, legal scoping.
2. **Platform foundation** — Spring Boot (Java 25 LTS, virtual threads) service template + shared libs; Spring Cloud Gateway + Spring Security (OIDC); accounts/consent; Kafka via Spring for Apache Kafka / Spring Cloud Stream; OpenTelemetry/Micrometer observability; Terraform IaC; Flutter app shell + Next.js web shell.
3. **Booking & payments** — NDC anti-corruption adapter, Booking service (virtual-threaded blocking I/O), PCI-isolated Payments service with tokenisation.
4. **Resilience intelligence** — feature pipeline, Python/FastAPI ML serving (unchanged), Resilience Score service (Spring) calling ML over HTTP/gRPC, accuracy harness.
5. **Disruption & recovery** — Spring WebFlux ingestion + detection state machine on the hot event path; Recovery Orchestrator saga (Axon or hand-rolled) with transactional-outbox; authority policy.
6. **Rights & claims** — versioned ruleset authoring + governance, effective-date eligibility engine, claims/payout (idempotent via outbox).
7. **Itinerary & sync** — CQRS read model (Spring Data + Redis projection), import/parse, downstream sync, Flutter offline-tolerant itinerary view.
8. **B2B2C** — partner API, white-label dashboard (Next.js), tenant isolation + metering.
9. **Quality & compliance** — JUnit 5 + Testcontainers + Spring Boot Test, Flutter widget/integration tests, contract tests (Spring Cloud Contract), SAST/DAST, privacy, audit, UAT.
10. **GTM enablement** — pilot onboarding, support tooling, runbooks.

---

## 3. Roles (RACI summary)

| Activity | Product | Eng | ML | Security/Compliance | Legal | Partner/GTM |
|---|---|---|---|---|---|---|
| Requirements (SRS) | A/R | C | C | C | C | C |
| Architecture (SDD) | C | A/R | C | C | I | I |
| Rights ruleset | C | R | I | A | R | I |
| Security/PCI/GDPR gate | I | C | I | A/R | C | I |
| Model accuracy gate | C | C | A/R | C | I | I |
| Pilot go-live | A | R | C | C | C | R |

A=Accountable, R=Responsible, C=Consulted, I=Informed.

---

## 4. Risk register (severity = Impact × Likelihood; from dossier §11)

| ID | Risk | I | L | Mitigation | Owner |
|---|---|---|---|---|---|
| R-1 | **Inventory access ≠ API access** — cannot execute cross-carrier rebooking without partner authority | H | H | Authority-gated design (SDD §5.2); assisted-MVP; pursue 2 pilots early | Partner/GTM |
| R-2 | **Regulatory moving target** — EU261 reform unresolved (trilogue); US DOT direction shifts | H | H | Versioned rules-as-data (ADR-2); compliance officer owns ruleset; effective-date eval | Compliance |
| R-3 | **B2C willingness-to-pay unproven** | H | M | Validate via survey before heavy build; lead with B2B2C revenue | Product |
| R-4 | **ML cold-start** — weak prediction pre-partner-data | M | H | Bootstrap on open OTP; publish measured AUC; degrade-safe (NFR-ACC-1) | ML |
| R-5 | **Channel conflict** with airlines/OTAs | H | M | White-label + revenue-share + post-booking positioning only | GTM |
| R-6 | **Liability** on assisted/automated rebooking & claims | H | M | Legal opinion gate before V1; explicit consent + audit (NFR-COMP-1) | Legal |
| R-7 | **Separate-ticket** economics unclear | M | M | V2 spike: parametric vs rebooking unit economics | Product |
| R-8 | **PCI/GDPR non-compliance** | H | L | Isolated PCI scope, region-pinned PII, external audit at each gate | Security |
| R-9 | **External API instability** (NDC/status) | M | M | Anti-corruption adapters, circuit breakers, degraded mode | Eng |

---

## 5. Test plan

### 5.1 Strategy & levels

| Level | Scope | Gate |
|---|---|---|
| Unit | Service logic, rules evaluators, score features | CI on every PR; coverage on critical paths |
| Integration | Adapter↔external (contract/mocked), event flows | CI nightly + pre-merge for core |
| Contract | NDC/payments/comms provider contracts | Provider sandbox |
| End-to-end | Booking, disruption→recovery, automatic-refund, claim | Pre-release per increment |
| ML evaluation | Resilience/misconnection model: ROC-AUC, calibration, drift, fairness across carriers/regions | Accuracy gate per phase (NFR-ACC-1) |
| Non-functional | Load (NFR-PERF), chaos/failover (NFR-AVL), surge auto-scale (NFR-SCAL) | Pre-release |
| Security | SAST/DAST, dependency scan, pen-test, OWASP ASVS L2 | Phase gate (R-8) |
| Compliance | Rights-decision correctness vs jurisdiction fixtures incl. effective-date branches; audit-log completeness | Compliance gate (R-2) |
| UAT | Pilot partner + traveller cohort | Pilot go-live |

### 5.2 Key test scenarios
- Effective-date rights branching: same disruption evaluated under pre- and post-EU261-reform rulesets returns version-correct outcomes.
- Idempotency: duplicated rebooking/payout requests cause exactly one effect (NFR-REL-1).
- IROPS storm: 5× surge → auto-scale ≤5 min, alerts still ≤2 min (NFR-PERF/SCAL).
- Authority gate: auto-recovery blocked when consent absent/expired/over-cap.
- US DOT automatic refund initiated within deadline on qualifying cancellation.

### 5.3 Entry/exit criteria
Entry: SRS-traced acceptance criteria defined, environments + sandboxes provisioned. Exit: all P0 FRs pass E2E, no open Critical/High defects, NFR thresholds met, security + compliance + accuracy gates signed.

---

## 6. Requirements Traceability Matrix (RTM)

| Req | Design (SDD) | Primary tests |
|---|---|---|
| FR-1 Booking | Booking Svc, NDC Adapter (§3, §6, ADR-7 — virtual-threaded blocking I/O) | Integration, contract, E2E booking |
| FR-2 Resilience Score | Resilience Svc, ML Serving (§3, §5) | Unit (features), ML eval, perf (P95≤800ms) |
| FR-3 Detection/alert | Ingestion, Detection SM (§3,§5.4, ADR-7 — WebFlux hot path) | Integration, E2E, surge load |
| FR-4 Recovery | Recovery Orchestrator, authority policy (§5.2, ADR-7 — saga + transactional-outbox) | E2E recovery, idempotency, authority-gate |
| FR-5 Rights/claims | Rights Engine, versioned ruleset (§5.1) | Compliance fixtures, effective-date branch |
| FR-6 Itinerary/sync | Itinerary Svc, CQRS (§3, ADR-5, ADR-8 — Flutter offline-tolerant view) | Integration, offline-mode, sync |
| FR-7 Separate-ticket | Rights Engine + parametric (§3,§10) | Scenario, unit economics (V2) |
| FR-8 Prefs/consent | Identity & Consent (§3) | Unit, consent-ledger audit |
| FR-9 Partner API | Partner Svc (§3, ADR-8 — Next.js white-label dashboard) | Contract, tenant-isolation, metering |
| FR-10 Payments | Payments Svc (§3, ADR isolated) | PCI, refund-deadline, idempotency |
| FR-11 Notifications | Notification Svc (§3) | Multi-channel fallback, priority lane |
| FR-12 Identity | Identity & Consent (§3) | Auth, RBAC, GDPR export/erase |
| NFR-* | Cross-cutting (§8, ADR-7 platform-wide perf/portability/maintainability, ADR-8 frontend/A11Y) | Non-functional & security suites |

---

## 7. Release, CI/CD & maintenance

- Trunk-based + feature flags; GitHub Actions pipeline (build→test→scan→deploy); IaC-managed environments (dev/stage/prod, EU+NA).
- Staged rollout for ruleset/model versions (canary by jurisdiction/tenant) with rollback.
- Post-release: SLO monitoring, model drift watch, monthly ruleset review against regulatory changes, quarterly security review, incident runbooks (IROPS storm, payout failure, rights-ruleset error).

---

## 8. Definition of Done (increment)

All in-scope FRs traced and E2E-passing · NFR thresholds met · security + privacy + compliance + ML-accuracy gates signed · documentation updated (SRS/SDD/RTM) · pilot/partner readiness confirmed for the phase.
