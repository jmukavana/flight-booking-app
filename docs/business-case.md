# Business Case & Feasibility Study

**Product:** Disruption-Resilient Flight Booking Platform (*Resilire*)
**Document:** Business Case v1.0 — BA artefact for investment / go-no-go decision
**Status:** Decision-support draft
**Last updated:** 2026-05-16
**Companion docs:** [`aviation-problem-definition.md`](./aviation-problem-definition.md) · [`SRS.md`](./SRS.md) · [`SDD.md`](./SDD.md) · [`SDLC-plan-and-test.md`](./SDLC-plan-and-test.md)
**Figures:** grounded against the dossier (§15 sources). Estimates carry stated assumptions; treat as decision ranges, not precision.

---

## 1. Executive recommendation

**Conditional GO — repositioned as B2B2C.** The problem is large, growing, and structurally unsolved; buyer budget on the B2B side is evidenced, not hypothetical. The venture has real-world weight **only if** the primary revenue engine is the white-label IROPS-resilience layer sold to airlines/OTAs/TMCs, with B2C as the data/UX flywheel — *not* a standalone consumer-subscription bet. Proceed to a funded validation phase gated by the four cheap tests in §7; do **not** commit full build spend until those pass.

---

## 2. Problem & opportunity (one-paragraph BA framing)

Booking platforms optimise a 3-minute purchase and abandon the 24–72h operational window where value is destroyed. IROPS costs the commercial-passenger system ~US$25–60bn/yr (definition-dependent), reliability is *declining* (US on-time 78.1% in 2024, cancellations 1.4%, both worse YoY), and disruption is rising ~3× faster than traffic. A reactive remedy industry (flight-delay-compensation, ~US$0.5–0.7bn, ~7% CAGR, consolidating) proves willingness to pay for *redress*; no one owns proactive, cross-carrier, booking-integrated *continuity*. That is the addressable white space.

---

## 3. Market sizing (assumptions stated)

| Layer | Estimate | Basis / assumption |
|---|---|---|
| **TAM** | ~US$1tn online air travel + ~US$25–60bn IROPS cost pool | IATA industry revenue >US$1tn (2025); IROPS cost range from dossier §5 |
| **SAM** | ~US$10–12bn | EU/NA frequent + business + multi-city/self-connect travellers, and mid-tier carriers/OTAs/TMCs seeking IROPS tech; strongest passenger-rights enforcement + NDC maturity |
| **SOM (3–5 yr)** | ~US$120–180m ARR | Blend: B2B2C SaaS (per-active-passenger + API tiers) as primary, B2C subscription/per-trip + ancillary/claims fee as secondary; assumes 30–60 B2B partners + a B2C cohort |

> BA caveat: SOM is scenario-dependent on partner conversion. The B2C-heavy variant is *lower-confidence*; the B2B2C-led variant is the defensible base case.

---

## 4. Revenue model & unit-economics assumptions

| Stream | Mechanism | Key assumption (to validate) |
|---|---|---|
| B2B2C SaaS (primary) | Per-active-passenger/month + API call tiers + white-label dashboard licence | Partners already budget IROPS self-service IT (~87% planning such spend) → land-and-expand via pilots |
| Ancillary / insurance commission | 10–20% on resilience add-ons + parametric protection | Attach rate on disrupted/at-risk itineraries |
| Claims processing fee | Transparent % of *successful* payout, user-approved | Reactive market proven (~US$0.5–0.7bn); margin vs AirHelp-class incumbents |
| B2C subscription / per-trip (secondary) | Freemium → paid auto-recovery & rights automation | **Unproven WTP — explicit validation gate (§7)** |

Cost structure (opex drivers): cloud + event infra, NDC/status/weather data licences, ML training/serving, **ongoing rights-ruleset maintenance** (a permanent compliance opex line, not one-off), security/PCI/GDPR audits, partner integration & support. Margin thesis: B2B2C SaaS gross margin is the engine; claims/ancillary are accretive; B2C is customer-acquisition + data, not the profit center at base case.

---

## 5. Competitive position (defensibility)

| Player | Owns | Gap they leave |
|---|---|---|
| Skyscanner / Google / OTAs | Pre-booking comparison | Zero post-purchase continuity |
| Hopper | Price psychology | Disruption = support cost, not product |
| AirHelp (incl. ClaimCompass) | Reactive claims monetisation | No prevention / no booking integration |
| Airline apps | Single-carrier recovery | No cross-carrier, siloed, legacy UX |
| **Resilire** | Prediction → prevention → recovery → rights → downstream sync | — (the unowned integration) |

**Moat:** (1) maintained multi-jurisdiction, version-aware **rights-rules asset**; (2) data flywheel (more partners → better inventory/prediction → better B2C → more partners); (3) B2B contractual inventory/authority access incumbents can't easily replicate.

---

## 6. Key risks (decision-relevant; full register in SDLC plan §4)

| Risk | Why it could kill the case | Containment |
|---|---|---|
| Inventory access ≠ API access | No cross-carrier rebooking without partner authority | Authority-gated design; assisted-MVP; early pilots |
| B2C WTP unproven | Erases a revenue line if assumed | Reposition B2B2C-led; validate before build |
| Regulatory churn (EU261 trilogue, US DOT shifts) | Compliance cost + moving target | Versioned rules-as-data; compliance owner |
| Channel conflict with airlines/OTAs | Partners refuse to integrate | White-label + revenue-share + post-booking only |
| Capital intensity / 18–24m runway | Under-funding = stall before flywheel | Stage-gate funding to validation milestones |

---

## 7. Go / No-Go scorecard

Weighted assessment. Each criterion scored **0–5** (0 = fails, 5 = strong); **Weighted = Score × Weight**. Decision thresholds in §8.

| # | Criterion | Weight | Score (now) | Weighted | Evidence / note |
|---|---|---:|---:|---:|---|
| C1 | Problem significance & growth | 0.15 | 5 | 0.75 | US$25–60bn, declining reliability, disruption > 3× traffic growth |
| C2 | Evidenced buyer budget (B2B) | 0.15 | 4 | 0.60 | ~87% airlines plan IROPS self-service IT; claims market proven |
| C3 | B2C willingness-to-pay | 0.10 | 1 | 0.10 | **Unvalidated** — primary open risk |
| C4 | Technical feasibility | 0.10 | 4 | 0.40 | Prediction tractable on open data; architecture defined (SDD) |
| C5 | Execution feasibility (partner authority) | 0.15 | 2 | 0.30 | Inventory/authority is commercial, not technical; needs pilots |
| C6 | Defensibility / moat | 0.10 | 4 | 0.40 | Rules asset + data flywheel + contractual access |
| C7 | Regulatory navigability | 0.10 | 3 | 0.30 | Fragmented + mid-reform; manageable as versioned data |
| C8 | Competitive whitespace | 0.05 | 5 | 0.25 | No incumbent owns the integrated value chain |
| C9 | Capital / runway realism | 0.10 | 3 | 0.30 | 18–24m runway; stage-gateable |
| | **Total** | **1.00** | | **3.40 / 5** | Equivalent to **68%** |

**Sensitivity:** if C3 (B2C WTP) validates to 4 → total ≈ 3.70 (74%). If C5 (partner authority) fails to ≤1 (no LOIs) → total ≈ 3.25 but **C5 is a hard gate** (see §8), triggering No-Go regardless of total.

---

## 8. Decision rule & gates

**Interpretation bands:** ≥3.6 = GO · 3.0–3.59 = **Conditional GO** (validate then re-score) · <3.0 = No-Go / pivot.
Current **3.40 → Conditional GO.**

**Hard gates (any failure ⇒ No-Go in current form, independent of total):**

| Gate | Pass condition | Cost | Owner |
|---|---|---|---|
| G1 — B2C WTP | ≥500-traveller survey + interviews show a viable paid segment OR B2B2C model holds without B2C revenue | Low | Product |
| G2 — Partner authority | ≥1 carrier + ≥1 OTA/TMC signed pilot LOI within validation phase | Low–Med | GTM |
| G3 — Tech proof | Resilience-Score AUC ≥ target on *open* OTP data (no partner data) | Low | ML |
| G4 — Legal | Counsel opinion: third-party assisted-rebooking & claims authority is workable per target jurisdictions | Low | Legal |

Validation phase = the 4 gates only (cheap, ~weeks, no full build). Re-score after; proceed to MVP (SDLC plan §1) only on a GO.

---

## 9. Recommendation summary

The project has genuine real-world weight as a **B2B2C IROPS-resilience platform**. It is **not** a credible standalone consumer-subscription play on current evidence. Fund the validation phase, enforce the four hard gates, and re-evaluate at a re-score checkpoint before committing build capital. Expected decision after gates: GO if G1–G4 pass; pivot-or-stop if G2 (partner authority) or G4 (legal) fail.
