# Disruption-Resilient Flight Booking — Aviation Sector Problem Definition

**Document type:** Industry problem analysis & research dossier
**Prepared from the perspective of:** Aviation distribution & passenger-experience specialist
**Status:** Deep-research draft for product, strategy, and fundraising use
**Last updated:** 2026-05-16
**Figures:** Grounded against public IATA / US DOT / EU institutions / industry analyst sources (see [§14 Sources](#14-sources)). Numbers are best-available public estimates; methodologies differ between bodies and are flagged where material.

---

## 1. Executive summary

The flight-booking industry is mature and ruthlessly optimised for one moment: the pre-purchase price comparison. It is structurally weak at the moment that actually destroys passenger trust and burns airline money — **the deviation from plan**. Delays, cancellations, missed connections, and denied boarding ("Irregular Operations", IROPS) are not edge cases; they are a structural, growing tax on a system carrying record traffic.

In 2024 global passenger demand hit an all-time high (≈9.4–9.5 billion airport passenger movements; ≈5 billion unique passenger journeys), yet US reporting carriers landed on time only **78.1%** of the time and cancelled **1.4%** of flights — *worse* than 2023 on both measures. Independent reviews of 2024–25 found delays and cancellations rising roughly **3× faster than traffic growth**, driven by congested hubs, ATC and labour shortages, and weather volatility. The recurring cost of this to the commercial passenger system is credibly estimated at **US$25–34 billion per year**, and as high as ~**8% of airline revenue (~US$60bn)** on broader definitions.

**Core problem (refined):** No mainstream booking platform treats disruption as a first-class, predictable, and automatable part of the product. Booking is sold as a transaction that ends at "payment confirmed", when in operational reality it is a 24–72-hour *liability window* the passenger is left to manage alone — across fragmented airline channels, opaque and now-shifting passenger-rights regimes, and downstream bookings (hotels, ground transport) that do not self-adjust.

**Why now:** Three enabling shifts have matured simultaneously — (a) NDC has crossed from pilot to ~60% airline adoption and ~20% of transactions, making programmatic re-shopping/rebooking feasible; (b) regulatory enforcement is intensifying and *diverging* (US DOT automatic-refund rule live since Oct 2024; EU261 reform in active trilogue with the Council and Parliament at odds), which raises both passenger awareness and airline compliance cost; (c) delay-prediction ML on open OTP + weather + airport data is now production-grade.

**The white space:** Incumbents own *pre-booking* (Skyscanner, Google Flights, OTAs). Claims firms (AirHelp, which has absorbed ClaimCompass) own *reactive failure monetisation*. Airlines own *single-carrier recovery only*. **No one owns proactive, cross-carrier, booking-integrated continuity.** That is the defensible position this venture targets.

---

## 2. Scope & definitions (precise taxonomy)

Sloppy terminology is the first failure of most "disruption" pitches. Precise definitions below; the product and its data model should use these.

| Term | Definition | Why it matters to the product |
|---|---|---|
| **IROPS** | Irregular Operations — any operation deviating from schedule: delay, cancellation, diversion, misconnection, denied boarding, tarmac hold. | The full problem surface; "delays" alone is too narrow. |
| **Controllable vs uncontrollable** | Airline-attributable (crew, maintenance, fleet routing) vs not (weather, ATC, security, force majeure). | Determines compensation eligibility under most regimes; central to the rights engine. |
| **Primary vs reactionary delay** | Primary = original root cause. Reactionary = knock-on delay propagated by shared aircraft/crew/slots. ~50%+ of European delay minutes are reactionary. | Prediction must model *propagation*, not just origin events. |
| **Misconnection (MCT breach)** | Inbound arrives too late to meet the **Minimum Connecting Time** at the hub. | The single highest-value preventable failure; computable *at booking time*. |
| **Denied boarding / oversale** | Passenger involuntarily offloaded from a confirmed seat. | Distinct rights regime, distinct UX. |
| **Single ticket vs separate tickets** | One PNR/contract of carriage vs self-connected itineraries. | On separate tickets the carrier has **no protection obligation** — a massive, under-served risk segment. |
| **Interline / codeshare / NDC order** | Contractual frameworks governing who may rebook a passenger onto whom. | Defines what the platform is legally and technically *allowed* to automate. |
| **GDS / PSS / NDC / ONE Order** | Distribution & reservation infrastructure (see §6). | Determines integration cost and rebooking capability. |

---

## 3. Market context (grounded)

- **Scale & growth.** Record demand: ≈5.0bn passenger journeys in 2024, forecast ≈5.2bn in 2025 (IATA, journey basis); ≈9.4–9.5bn passenger movements on airport/ICAO-ACI basis (the gap is methodology — connections double-count on a movement basis; the product must reason in *journeys and segments*, not headline figures). Industry revenue first crossed **US$1 trillion** in 2025.
- **Reliability is not improving with recovery.** US 2024: on-time arrival **78.1%** (down from 78.34% in 2023); cancellations **1.4%** (up from 1.3%); 437 domestic tarmac delays >3h. Global OTP leaders sit only in the **83–87%** band — i.e. even the best large carriers fail ~1 in 7 flights on time.
- **Disruption is outpacing growth.** 2024–25 reviews: traffic +6–7% YoY while delays/cancellations rose ~3× faster — a structural deterioration tied to hub saturation, ATC/staffing shortfalls, and weather.
- **The recovery economy already exists but is reactive.** Flight-delay-compensation services were a **~US$0.5–0.7bn** market in 2024, ~7% CAGR toward ~US$1.4bn by 2033; consolidating (AirHelp acquired ClaimCompass; 60+ airlines run embedded claims modules). Demand for *remedy* is proven; the gap is *prevention and continuity*.

---

## 4. Problem statement

> **Travellers buy a transaction but live a journey.** Booking platforms optimise the 3-minute purchase and abandon the 24–72-hour operational window in which value is actually delivered or destroyed. When operations deviate — which they do on a material share of itineraries — the passenger is forced into manual, fragmented, time-pressured recovery across siloed airline channels, with opaque and currently *shifting* compensation rights, while downstream bookings silently break. Simultaneously, airlines and OTAs absorb tens of billions in IROPS support, re-accommodation, and regulatory cost, and lack a scalable, passenger-trusted layer to reduce it.

**Sharper sub-problems (each independently fundable):**

1. **Risk is invisible at the point of sale.** Two itineraries at the same price can have wildly different misconnection/cancellation probability; no mainstream platform surfaces this.
2. **Recovery is manual and serial.** The passenger discovers disruption late, then queues (phone/airport/app) against thousands of others for finite reaccommodation inventory.
3. **Rights are opaque and now unstable.** EU261 vs UK261 vs US DOT vs Canada APPR vs Brazil ANAC differ by route, cause, and duration — and the EU regime itself is mid-reform (see §8). Passengers leave money unclaimed; airlines carry compliance risk.
4. **The itinerary is not a system.** Flights, hotels, ground transport, and insurance are independent bookings with no shared state; a flight change does not propagate.
5. **Separate-ticket and self-connect travellers are unprotected by design** — a fast-growing segment (LCC + multi-app booking behaviour) with *zero* carrier safety net.

---

## 5. Quantifying the problem (the cost stack)

Public estimates vary by scope; presented as a layered stack rather than a single contested number.

| Cost layer | Best-available public estimate | Borne by |
|---|---|---|
| Recurring commercial-passenger delay system | ~US$25bn (IATA framing) to ~US$34bn/yr | Airlines + passengers |
| Broad definition incl. schedule padding & reactionary | up to ~8% of airline revenue (~US$60bn) | Airlines |
| US domestic disruption (2022 reference) | ~US$30–34bn total; ~US$12bn passenger time; ~US$11bn airline incremental | Split |
| Compensation/claims handling market | ~US$0.5–0.7bn (2024), growing ~7%/yr | Airlines (payouts) + claims intermediaries |
| Crew/passenger reaccommodation effort | 2–4h staff effort for a 20–30 flight event; 6–12h+ for 100+ flights | Airlines |

**Operational lever proven by the data:** proactive communication during large IROPS reduces inbound call volume by **up to ~70%**, and **87% of airlines plan IROPS self-service IT investment within 3 years**. This is the quantified wedge: the market is *already going to spend* on this; the question is who owns the passenger-facing layer.

---

## 6. Root-cause analysis (across the distribution & operations stack)

Why has a trillion-dollar industry not solved this? Because the failure is structural, not a missing feature.

1. **Legacy distribution architecture.** GDS (Amadeus, Sabre, Travelport) and airline PSS were built for *inventory and fare distribution*, not dynamic passenger recovery. Rebooking authority and re-accommodation logic live inside each airline's PSS/DCS.
2. **NDC is necessary but not sufficient.** NDC enables programmatic offers/orders and is now ~60% airline-adopted / ~20% of transactions — but only **~27% of airlines have meaningful Offer & Order (ONE Order) maturity**. Cross-carrier *automated rebooking* still hits contractual and technical walls.
3. **Data silos with no passenger-actionable standard.** Airlines, airports, ANSPs/ATC, GDSs, and OTAs do not share real-time operational state in a standardised, consent-bound, passenger-facing format. Prediction is possible from external signals; *execution* requires inventory access.
4. **Liability & control asymmetry.** Airlines guard rebooking authority because they own the contract of carriage and the compensation liability. A third party automating reaccommodation must solve *authority delegation*, not just APIs.
5. **Interline/codeshare/separate-ticket fragmentation.** Whether a passenger *can* be moved to another carrier depends on interline agreements, the ticket structure, and fare rules — invisible to the passenger and most platforms.
6. **Incentive misalignment.** Booking platforms monetise *conversion and commission*. Post-booking support is a cost centre, not a revenue line — so it is systematically under-invested. (This is precisely the misalignment a resilience-priced product inverts.)
7. **Regulatory fragmentation *and now instability*** (see §8) — building a static rules engine is a moving target.

---

## 7. Stakeholder impact

| Stakeholder | Core pain | Economic / strategic consequence |
|---|---|---|
| **Passengers** | Stress, missed connections, financial loss, opaque/shifting rights, unprotected self-connects | Trust erosion; willingness to pay for certainty; unclaimed compensation |
| **Airlines** | Call-centre surge, brand damage, inefficient recovery, regulatory penalties, compensation payouts | Billions in IROPS cost; DOT/EU enforcement exposure; NPS damage |
| **OTAs / booking platforms** | Post-booking support tickets can exceed booking margin during IROPS; low post-sale engagement | Margin compression; churn; no retention asset |
| **Airports** | Terminal congestion, degraded passenger flow during mass disruption | Operational strain; lost retail/F&B revenue |
| **Corporates / TMCs** | Duty-of-care obligations; traveller productivity loss; manual rebooking | Direct cost + liability; strong B2B willingness to pay |
| **Insurers** | Manual, adversarial claims; loss-ratio volatility | Demand for embedded, data-rich parametric products |

---

## 8. Regulatory landscape (specialist deep-dive — the highest-value section)

A resilience product *is* partly a compliance product. The regime is fragmented **and the largest regime is mid-reform**, which is both a risk and the venture's strongest tailwind.

**European Union — Regulation 261/2004 ("EU261"), under active revision.**
- Current regime: compensation €250–€600 by distance for delays ≥3h, cancellation, and denied boarding, subject to "extraordinary circumstances" carve-outs.
- **Reform status (live):** Council adopted its first-reading position **5 June 2025**, proposing to raise the delay-compensation threshold to **4h (short-haul) / 6h (long-haul)** plus tarmac-disembarkation and rerouting-via-other-carriers/modes rules. The **European Parliament adopted its position (Jan 2026, 632–15)** *rejecting* the higher thresholds and defending the **3h** trigger. The file is now in **trilogue** (Parliament/Council/Commission) — outcome and effective date unresolved. IATA has publicly opposed the direction.
- **Product implication:** the rights engine must be **versioned and effective-date-aware**, not hard-coded; the regulatory churn itself increases passenger confusion → demand for an automated interpreter.

**United Kingdom — UK261.** Retained post-Brexit analogue of EU261 (CAA-enforced); diverging independently. Must be modelled as a separate jurisdiction, not "EU".

**United States — DOT, materially strengthened in 2024.**
- **Automatic Refund Final Rule** (issued Apr 2024; reinforced by the FAA Reauthorization Act, signed 16 May 2024; key provisions effective **28 Oct 2024**): airlines must *automatically* refund — without the passenger asking — for cancellations and *significant* changes the passenger declines, and for **paid ancillaries not delivered** (Wi-Fi, seat, IFE). Refunds within 7 business days (card) / 20 days (other), in original form of payment.
- DOT has also opened rulemaking on compensation/assistance for controllable disruptions (status evolving with administration changes — track, do not assume).
- **Product implication:** "did the airline owe an automatic refund and actually pay it, correctly and on time?" is now a *concrete, automatable US entitlement* — a wedge feature distinct from EU-style compensation.

**Canada — Air Passenger Protection Regulations (APPR).** Tiered compensation by carrier size and controllability; under its own strengthening cycle. Separate jurisdiction model.

**Brazil — ANAC Resolution 400 et al.**, and a long tail of national/bilateral regimes. The defensible asset is a **maintained, jurisdiction- and version-aware rules graph**, keyed on (origin, destination, operating/marketing carrier, cause, duration, ticket structure, effective date).

---

## 9. Demand-side & behavioural evidence

- **Reliability is becoming a purchase criterion**, not just price — amplified by remote/"bleisure" work, multi-city itineraries, and tighter self-connections (more MCT-fragile journeys).
- **Proven willingness to pay for remedy**: a profitable, growing claims industry exists *purely on the reactive side* — strong evidence travellers value money-back-for-disruption; the untested-but-adjacent hypothesis is willingness to pay for *prevention/continuity ex ante*.
- **Loss aversion & control**: disruption stress is dominated by *uncertainty and lack of agency*, not only the delay itself — implying the product's core value is **proactive certainty and one-tap agency**, which is a UX/communications problem as much as an inventory problem.
- **Open validation gap (do not assume):** B2C standalone willingness-to-pay for a subscription is unproven. The strongest near-term monetisation evidence points to **B2B/B2B2C** (airlines/OTAs/TMCs already budgeting IROPS self-service spend).

---

## 10. Competitive white space

| Capability | Metasearch/OTA (Skyscanner, Google, Hopper) | Claims firms (AirHelp/ClaimCompass) | Airline apps | **This venture** |
|---|---|---|---|---|
| Pre-booking price/inventory | ✅ Strong | ❌ | ⚠️ Own only | ➖ Secondary |
| **Disruption risk at point of sale** | ❌ | ❌ | ⚠️ Internal only | ✅ Multi-factor, cross-carrier |
| **Proactive, cross-carrier recovery** | ❌ | ❌ | ⚠️ Own inventory only | ✅ NDC-enabled, assisted→automated |
| **Rights: proactive eligibility + claim** | ❌ | ✅ but *reactive* | ⚠️ Manual/opaque | ✅ Proactive, multi-jurisdiction, version-aware |
| **Unified self-adjusting itinerary** | ⚠️ Flights only | ❌ | ⚠️ Own flights | ✅ Flights+stay+ground+insurance |
| **B2B/white-label IROPS layer** | ❌ | ⚠️ Claims only | ❌ | ✅ SaaS + API + shared inventory |

**Insight:** every incumbent monetises one slice (conversion / failure / single-carrier ops). None integrates *prediction → prevention → automated recovery → rights → downstream sync*. That integration is the moat (data flywheel + regulatory-rules asset + B2B inventory access).

---

## 11. Why this is hard (the honest constraints / "graveyard")

A specialist must state why prior attempts stalled — these are the real risks:

1. **Inventory access ≠ API access.** Predicting disruption is tractable from public signals; *executing* a cross-carrier rebooking requires commercial agreements and delegated authority airlines are reluctant to grant a third party.
2. **Authority & liability.** Automated rebooking touches the contract of carriage. MVP must be **assisted with explicit user confirmation + pre-authorised criteria**; full automation is a *partnership/regulatory* unlock, not an engineering one. Pitches that promise "instant auto-rebooking" on day one lose credibility with airline counterparts.
3. **Separate-ticket reality.** The most disruption-exposed travellers are often the hardest to protect (no carrier obligation) — solving them may require *insurance/parametric* mechanisms, not just rebooking.
4. **Regulatory moving target.** EU261 reform unresolved; US DOT direction administration-sensitive. The rules engine is an *ongoing maintained asset*, a real opex line, not a one-off build.
5. **Channel conflict.** A passenger-side product that reroutes off the booked carrier threatens airline/OTA economics — mitigated only by white-label + revenue-share + post-booking (not pre-booking) positioning.
6. **Cold-start data.** Prediction quality and rebooking speed improve with scale and B2B inventory access — a classic flywheel that is weakest at launch.

---

## 12. Problem → solution requirements (traceability)

Not a solution design — the *requirements any credible solution must satisfy*, derived from the problem.

| Problem (§4) | Non-negotiable requirement |
|---|---|
| Risk invisible at sale | A per-itinerary, explainable **Resilience/Misconnection score** computable pre-purchase from OTP history, MCT/connection buffer, weather, hub congestion |
| Manual, serial recovery | Event-driven detection (minutes, not airport-screen latency) + ranked recovery options + one-tap assisted action with pre-authorised criteria |
| Opaque, unstable rights | **Versioned, effective-date-aware, multi-jurisdiction rights engine** (EU261/UK261/DOT/APPR/ANAC…) keyed on route, carrier, cause, duration, ticket structure |
| Itinerary not a system | Single itinerary state with webhooks that propagate flight changes to stay/ground/insurance |
| Unprotected self-connects | Explicit separate-ticket risk detection + parametric/insurance fallback path |
| Liability/authority limits | Architecture that degrades gracefully: assisted-with-consent today, automated where partnership/authority exists |
| Cold-start & moat | B2B2C design so airline/OTA/TMC partners supply inventory + data that improve the B2C flywheel |

---

## 13. Validated vs open questions (research agenda)

**Reasonably validated by public evidence:**
- IROPS is a large, *growing*, structurally-caused cost (§3, §5).
- Reactive remedy demand is real and monetisable (claims market, §3).
- Regulatory pressure is rising and increases passenger confusion → demand for interpretation (§8).
- Airlines are already committing IROPS self-service IT spend (§5) — B2B budget exists.

**Open — must be primary-researched before heavy build:**
1. B2C standalone willingness-to-pay (subscription vs per-trip vs free+ancillary) — survey ≥500 frequent/business travellers.
2. Which **2–3 B2B pilot partners** (regional/LCC carrier, mid-OTA, TMC) will grant sandbox inventory + co-develop recovery flows.
3. Achievable misconnection-prediction accuracy on *open* datasets pre-partnership (build a measurable v1).
4. Legal opinion on third-party assisted-rebooking authority and claims submission per jurisdiction.
5. Separate-ticket protection mechanism: parametric insurance vs rebooking — unit economics of each.

---

## 14. Recommended immediate next steps

1. **Demand validation:** structured survey + 15–20 interviews (frequent, business, multi-city, self-connect segments) on WTP and trigger moments.
2. **Data/API access applications:** NDC sandbox (Duffel / Amadeus Self-Service as MVP proxy for direct GDS/NDC), FlightAware/OAG OTP, weather/ATC feeds.
3. **Resilience Score v1:** build and back-test on open OTP + connection-buffer data; publish a measured accuracy figure (credibility asset for partner and investor conversations).
4. **Rights-engine spike:** model EU261 (with reform branches) + UK261 + US DOT 2024 automatic-refund rule as a versioned ruleset; this doubles as the compliance differentiator.
5. **Legal/regulatory counsel:** scope assisted-rebooking and claims authority per jurisdiction.
6. **Identify 2 B2B pilots:** one regional/LCC carrier + one mid-tier OTA or TMC for an IROPS-management co-development.

---

## 15. Sources

Public sources consulted for grounding (figures are public estimates; methodologies differ between bodies):

- IATA — record passenger demand 2024 / 2025 outlook & >US$1tn revenue: https://www.iata.org/en/pressroom/2025-releases/2025-01-30-01/ ; https://www.iata.org/en/pressroom/2024-releases/2024-12-10-01/ ; https://www.iata.org/en/pressroom/2025-releases/2025-08-04-01/
- ACI World / ICAO passenger traffic basis: https://aci.aero/2025/01/28/joint-aci-world-icao-passenger-traffic-report-trends-and-outlook/
- US DOT — Air Travel Consumer Report, full-year 2024 (OTP 78.1%, cancellations 1.4%, tarmac delays): https://www.transportation.gov/briefing-room/air-travel-consumer-report-december-2024-full-year-2024-numbers
- Flight-delay cost estimates (~US$25–34bn; ~8%/~US$60bn broad): https://www.thetraveler.org/flight-delays-now-cost-global-aviation-34-billion-a-year/ ; https://www.switchfly.com/blog/cancelled-flights-and-hotel-vouchers-irop-disruptions-passenger-point-view ; https://amadeus.com/en/blog/articles/top-ten-common-causes-airline-disruptions
- US DOT Automatic Refund Final Rule (Apr 2024; FAA Reauthorization 16 May 2024; effective 28 Oct 2024): https://www.transportation.gov/briefing-room/biden-harris-administration-announces-final-rule-requiring-automatic-refunds-airline ; https://www.transportation.gov/airconsumer/refundsfinalruleapril2024 ; https://www.federalregister.gov/documents/2024/04/26/2024-07177/refunds-and-other-consumer-protections
- EU261 reform — Council position 5 Jun 2025: https://www.consilium.europa.eu/en/press/press-releases/2025/06/05/council-sets-position-on-clearer-and-improved-rules-for-air-passengers/
- EU261 reform — Parliament position & trilogue: https://www.europarl.europa.eu/news/en/press-room/20260116IPR32442/european-parliament-stands-behind-air-passenger-rights ; https://www.europarl.europa.eu/legislative-train/spotlight-JD22/file-common-rules-on-compensation-to-passengers ; https://www.iata.org/en/pressroom/2025-releases/2025-11-18-01/
- NDC adoption (~60% airlines / ~20% transactions / ~27% Offer-Order maturity / IATA ~10% agency sales): https://www.iata.org/en/programs/airline-distribution/retailing/ndc/ ; https://www.travelweekly.com/Travel-News/Airline-News/IATA-says-NDC-adoption-reaches-10-percent ; https://www.altexsoft.com/blog/iata-one-order-for-the-airlines/
- Flight-delay-compensation market size & consolidation (AirHelp/ClaimCompass): https://www.marketgrowthreports.com/market-reports/flight-delay-claim-service-market-113303 ; https://therecursive.com/world-s-largest-flight-compensation-company-acquires-bulgarian-founded-claimcompass/
- IROPS recovery effort / call-volume reduction / airline IT investment intent: https://www.flightglobal.com/analysis-it-trends-survey-highlights-irops-focus/113629.article ; https://routespring.com/how-airlines-handle-irops-crew-rebooking ; https://www.voyageraid.net/blog/how-airlines-can-fix-disruption-communication-with-automation/

> **Caveat:** Several cost figures originate from secondary aggregators citing IATA/consultancy work; treat the **range** ($25–60bn depending on definition) as the credible position and avoid quoting a single precise number in investor materials without primary citation.
