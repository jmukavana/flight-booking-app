# Software Requirements Specification (SRS)

**Product:** Disruption-Resilient Flight Booking Platform (working name: *Resilire*)
**Document:** SRS v1.0 — IEEE 830 / ISO/IEC/IEEE 29148 aligned
**Status:** Baseline draft for design & estimation
**Last updated:** 2026-05-17
**Companion docs:** [`aviation-problem-definition.md`](./aviation-problem-definition.md) (problem dossier) · [`SDD.md`](./SDD.md) (design) · [`SDLC-plan-and-test.md`](./SDLC-plan-and-test.md) (plan, risk, test, traceability)

---

## 1. Introduction

### 1.1 Purpose
Defines the functional and non-functional requirements for a resilience-first flight booking platform that **predicts, prevents, and automates recovery from travel disruption (IROPS)** and manages passenger-rights compensation, delivered as a B2C app and a B2B2C white-label layer. Audience: engineering, QA, product, security/compliance, and partner integration teams.

### 1.2 Scope
Derived directly from the problem→requirement traceability in the dossier (§12). In scope: flight search/booking via NDC/aggregator, pre-purchase Resilience Score, real-time disruption detection, assisted/automated recovery, multi-jurisdiction rights & claims engine, unified self-adjusting itinerary, separate-ticket risk handling, partner API/dashboard, payments/refunds/payouts. Out of scope for v1: direct GDS PSS ownership, full unattended cross-carrier auto-rebooking without partner authority, non-air primary booking.

### 1.3 Definitions
Uses the taxonomy in dossier §2 (IROPS, MCT, primary/reactionary delay, single vs separate ticket, NDC/PSS/ONE Order, controllable vs uncontrollable). Acronyms: **SRS** Software Requirements Spec, **FR** Functional Requirement, **NFR** Non-Functional Requirement, **UC** Use Case, **RTM** Requirements Traceability Matrix, **SLA/SLO** service level agreement/objective.

### 1.4 References
IEEE 830-1998; ISO/IEC/IEEE 29148:2018; EU Reg. 261/2004 (and 2025–26 reform); UK261; US DOT Automatic Refund Final Rule (effective 28 Oct 2024); Canada APPR; PCI-DSS v4.0; GDPR; WCAG 2.2 AA; IATA NDC 21.3+/ONE Order. Full source URLs in dossier §15.

### 1.5 Constraints & assumptions
- Inventory access ≠ API access: automated cross-carrier rebooking is gated by commercial agreements (dossier §11). v1 = **assisted with explicit consent + pre-authorised criteria**.
- Rights regimes are fragmented **and mid-reform** → rules must be data-driven and versioned, never hard-coded.
- MVP uses Duffel / Amadeus Self-Service as NDC proxy; direct NDC/GDS is a partnership unlock.

---

## 2. Overall description

### 2.1 Product perspective
A new, standalone, cloud-native platform with an event-driven core. It integrates externally with NDC aggregators, flight-status/weather/airport feeds, payment processors, communications providers, and partner systems. It does **not** replace airline PSS/DCS; it orchestrates on top of them.

### 2.2 Actors / user classes
| Actor | Description | Privilege |
|---|---|---|
| Traveller (B2C) | Books, manages itinerary, recovers from disruption, claims compensation | Standard |
| Corporate traveller / arranger | Books within policy; duty-of-care visibility | Standard + policy scope |
| Partner (airline/OTA/TMC) | Consumes white-label API/dashboard | Tenant-scoped |
| Operations/admin | Monitors disruptions, overrides, supports users | Elevated |
| Compliance officer | Maintains rights ruleset, audits claims | Elevated, segregated |
| ML pipeline (system actor) | Trains/serves prediction & optimisation models | Service |
| External services (system actors) | NDC, status, weather, payments, comms | Service |

### 2.3 Operating environment
iOS/Android (Flutter), modern web (Next.js); Spring Boot (Java 25 LTS) services on AWS (EKS), multi-AZ, EU + NA regions for data residency. Browsers: last 2 major versions. Offline-tolerant itinerary view.

### 2.4 Design & implementation constraints
Microservices + event streaming (Kafka). Stateless services behind API gateway. PCI scope isolated. GDPR data residency by region. All rights logic externalised as versioned rule data.

---

## 3. Functional requirements

Priority: **P0** = MVP, **P1** = V1, **P2** = V2. Each FR is testable and traced in [`SDLC-plan-and-test.md`](./SDLC-plan-and-test.md) §RTM.

### FR-1 Flight search & booking — P0
1. Search one-way/round/multi-city via NDC aggregator + airline-direct where available.
2. Return fare, fare rules, ancillaries, and ticket structure (single vs separate-ticket flagged).
3. Create order/PNR, capture payment, issue confirmation, persist itinerary state.
4. Support cancellation/refund per fare rules and applicable regulation (see FR-5, FR-10).

### FR-2 Resilience Score — P0
1. Compute an explainable 0–100 score per itinerary **before purchase** from: historical OTP, connection buffer vs MCT, weather forecast, hub congestion, aircraft-rotation/reactionary risk.
2. Display score + top contributing factors + per-segment misconnection probability.
3. Recompute on itinerary change; expose via partner API (FR-9).
4. Score model must be versioned and its accuracy measurable (see NFR-ACC).

### FR-3 Disruption detection & alerting — P0
1. Ingest real-time flight-status/weather/airport events.
2. Detect delay, cancellation, predicted misconnection, diversion, denied boarding, tarmac hold for any active itinerary.
3. Emit a domain event and notify the affected user within **≤2 min** of source signal via push/SMS/email/WhatsApp with impact + next steps + "Assist Me".
4. Suppress duplicate/conflicting alerts; maintain a single authoritative disruption state per itinerary.

### FR-4 Recovery / assisted rebooking — P0 (assisted) / P1 (automated)
1. On disruption, generate ranked recovery options (rebook same/other carrier, alternative mode, refund) respecting fare rules, interline/codeshare eligibility, loyalty, and user pre-auth criteria.
2. P0: present options; user confirms → create rebooking draft; execute via NDC/aggregator or hand-off; preserve ancillaries where possible.
3. P1: if user opted into Auto-Recovery (FR-8) and criteria match, execute automatically with idempotency and a full audit trail.
4. Never execute an irreversible action without satisfying the configured authority/consent rule.

### FR-5 Passenger-rights & compensation engine — P1
1. Determine eligibility from a **versioned, effective-date-aware, multi-jurisdiction ruleset** (EU261 incl. reform branches, UK261, US DOT automatic-refund rule, Canada APPR, Brazil ANAC, extensible), keyed on origin/destination, operating+marketing carrier, cause (controllable/uncontrollable), duration, ticket structure, date.
2. Auto-detect entitlement (compensation and/or mandatory automatic refund) per active disruption.
3. Generate, submit, and track claims; route approved payouts (FR-10) with transparent fee disclosure and user approval.
4. Every eligibility decision is explainable and audit-logged with the ruleset version applied.

### FR-6 Unified itinerary hub & downstream sync — P0 (hub) / P1 (sync)
1. Aggregate flights, stays, ground transport, insurance into one itinerary (import via email-forward parsing, confirmation, or manual add).
2. Maintain single itinerary state; offline-viewable.
3. P1: on flight change, propagate to downstream bookings (notify/auto-adjust where partner APIs allow) and surface required user actions.

### FR-7 Separate-ticket risk & parametric fallback — P1
1. Detect self-connected/separate-ticket itineraries and flag the absence of carrier protection at booking and on disruption.
2. Offer a parametric protection product (insurance/embedded) as fallback recovery path; manage policy lifecycle and automatic payout trigger.

### FR-8 User preferences & auto-recovery authorisation — P1
1. Per-trip configurable recovery criteria: max layover, cabin floor, budget delta, preferred carriers/alliances, refund-vs-rebook preference.
2. Explicit, revocable Auto-Recovery consent with scope and spend cap; immutable consent audit record.

### FR-9 Partner API & white-label dashboard (B2B2C) — P1
1. Tenant-isolated REST/event API exposing Resilience Score, disruption events, recovery options, rights eligibility.
2. White-label dashboard: live disruption view, reaccommodation queue, support-deflection and SLA metrics per tenant.
3. Usage metering for billing (per active passenger / API tier).

### FR-10 Payments, refunds, payouts — P0
1. Multi-currency capture (card/wallet), 3-DS/SCA where required; PCI-DSS tokenisation, no PAN in app scope.
2. Refunds in original form of payment within regulatory deadlines (US DOT: 7 business days card / 20 days other) with automatic-refund detection where mandated.
3. Compensation payout disbursement with transparent, user-approved fee; full ledger.

### FR-11 Notifications — P0
Multi-channel (push/SMS/email/WhatsApp) with channel fallback, user-controlled preferences/quiet hours, localisation, and delivery-status tracking. Disruption alerts are highest priority and bypass quiet hours.

### FR-12 Identity, accounts & consent — P0
Registration/login (email + OAuth/SSO), MFA option, profile, GDPR consent & data-subject rights (export/delete), partner SSO for B2B tenants, RBAC for internal actors.

---

## 4. Non-functional requirements

| ID | Category | Requirement (measurable) |
|---|---|---|
| **NFR-PERF-1** | Latency | Search results P95 ≤ 3 s; Resilience Score P95 ≤ 800 ms; disruption alert end-to-end ≤ 2 min |
| **NFR-PERF-2** | Throughput | Sustain 1,000 searches/s and 50k concurrent active monitored itineraries at launch scale; horizontally scalable |
| **NFR-AVL-1** | Availability | Core booking & disruption services 99.9% monthly; disruption detection pipeline 99.95% (degraded-mode tolerated, data-loss not) |
| **NFR-SCAL-1** | Elasticity | Auto-scale to 5× baseline within 5 min for IROPS storms (mass-event surge) |
| **NFR-SEC-1** | Security | OWASP ASVS L2; TLS 1.2+ in transit, AES-256 at rest; secrets in managed vault; least-privilege IAM |
| **NFR-SEC-2** | Payments | PCI-DSS v4.0 compliant; cardholder data confined to tokenisation provider scope |
| **NFR-PRIV-1** | Privacy | GDPR/CCPA compliant; EU PII stays in EU region; data-subject export/erasure ≤ 30 days; consent ledger immutable |
| **NFR-COMP-1** | Auditability | Every rights decision, recovery action, payout, and consent change is immutably logged with actor, timestamp, ruleset/model version |
| **NFR-ACC-1** | Model accuracy | Resilience/misconnection model: published, monitored metric (target ≥0.80 ROC-AUC on holdout at v1); drift-monitored; degrades safe (flag, don't fabricate) |
| **NFR-REL-1** | Reliability | All recovery/booking actions idempotent; exactly-once effect under retry; no double-booking |
| **NFR-OBS-1** | Observability | Distributed tracing, structured logs, SLO dashboards, alerting; mean-time-to-detect incident ≤ 5 min |
| **NFR-I18N-1** | Localisation | UI + notifications localisable; multi-currency, timezone-correct itinerary rendering |
| **NFR-A11Y-1** | Accessibility | WCAG 2.2 AA for web and mobile |
| **NFR-MNT-1** | Maintainability | Rights ruleset updatable as data (no code deploy) with versioning and effective dates; staged rollout |
| **NFR-PORT-1** | Portability | Cloud-portable (containerised, IaC); no single-vendor lock for core compute |

---

## 5. External interfaces

- **NDC / aggregator** (Duffel / Amadeus Self-Service MVP; direct NDC/airline post-partnership): search, order, rebook, refund.
- **Flight status & ops data**: FlightAware/OAG-class feeds; airport/ATC where available.
- **Weather**: forecast + nowcast provider.
- **Payments**: Stripe (primary) + secondary processor; PCI tokenisation.
- **Communications**: SMS/WhatsApp, email, push (FCM/APNs).
- **Partner integration**: outbound REST + webhooks + event stream, tenant-scoped OAuth2.

---

## 6. Acceptance criteria (sample, per FR)

- **FR-2:** For a known historical misconnection itinerary, score is in the high-risk band and lists connection-buffer as a top factor; recompute on date change is reflected ≤1 s.
- **FR-3:** Inject a simulated cancellation event → affected user receives a multi-channel alert with "Assist Me" within 2 minutes; no duplicate alerts.
- **FR-5:** For an EU-departing controllable 4-hour delay, engine returns the correct entitlement under the *applicable effective-dated* ruleset, with an explainable trace and recorded ruleset version.
- **FR-10:** A US-itinerary cancellation with no rebooking triggers automatic refund detection and a refund initiated within the regulatory deadline.

Full acceptance & test mapping: [`SDLC-plan-and-test.md`](./SDLC-plan-and-test.md).
