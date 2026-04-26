# TBO Air Integration — Design & Rollout Plan

**Status:** Approved design, Week-0 foundation built (commits `ea209b3` + V45 country migration). Awaiting TBO sandbox creds to begin live integration.
**Author:** Sawan + Claude (BMAD brainstorm session 2026-04-26)
**Source brainstorm:** `_bmad/docs/brainstorming/brainstorming-session-2026-04-26-tbo-design-plan.md`

---

## 1. Executive Summary

Safar is integrating **TBO Air** as the Indian-content primary flight provider. Duffel (already live) is demoted to international-only after the discovery that Duffel's Indian airline content is thin. Amadeus (already live) remains as failover. The integration includes a **Universal Trip ID** that ties flight + stay + cab + cook + insurance into one cross-vertical bundle — Safar's actual moat versus MMT/RedBus/IRCTC.

**Strategic stack:**

| Provider | Role | Status |
|---|---|---|
| **TBO Air** | Indian content primary (LCC + full-service) | Adapter scaffolded; awaiting sandbox creds |
| **Duffel** | International + NDC content | Live (production-ready) |
| **Amadeus** | Failover for both India and international | Live (free tier) |

**Non-negotiable design choice:** Country/state/region fields added to Trip schema Day 1 (V45 migration) so the same engine works when Safar expands to UAE / SG / SE Asia in 2027-28.

---

## 2. Design Space (8-Parameter Morphological Matrix)

| # | Parameter | Decision |
|---|---|---|
| **1** | **Commercial** | Wallet ramp ₹50k → ₹2L over 90 days, prepaid → credit-line, dynamic markup (₹150-250 full-service / ₹75 LCC), TBO as agent of record for GST |
| **2** | **Technical adapter** | Full lifecycle Day 1, geography-routed search (TBO India + Duffel intl + Amadeus failover), cached session auth (~24h), hybrid retry (3× transient with backoff) |
| **3** | **Rollout** | Canary 10/50/100 over 3 weeks, cohort pilot first (5k existing PG/hotel users), dark→soft→full launch sequence |
| **4** | **Operational** | Auto-refund ≤ ₹10k for refundable fares, push+poll for schedule changes, hybrid customer support, mirrored PNR + daily reconciliation |
| **5** | **Risk & monitoring** | Daily recon (financial + status), full instrumentation, Amadeus failover when TBO down, full request/response logging Day 1 (sampled to 5% after 90 days) |
| **6** | **Customer support** | Extend PG ticket system (existing S26); 4 SLA tiers (4h/24h/48h/72h); 3-level escalation L1 ops → L2 supervisor + TBO L1 → L3 TBO account manager + airline desk |
| **7** | **Notifications** | MSG91 WA + SMS + email + push + in-app; per-event channel mix; EN+HI Day 1; smart dedupe within 1hr |
| **8** | **Cross-vertical engine** | Confirmation + 2-day-before triggers; rule-based logic Day 1 → ML in Phase 3; "Complete your trip" hub; Universal Trip ID built upfront; 8-12% bundle discount; **Day 1 verticals = Stay + Cab + Cook + Insurance**; Phase 2 verticals = Pandit + Decor + Spa + Experiences |

---

## 3. Decision Trees

### Tree 1 — 90-day rollout sequence

```
Week 0 (NOW)        Send partner emails; build adapter scaffold + Trip schema
                    + suggestion rules. (Done in commit ea209b3 + V45.)
Weeks 1-3           Wait for TBO sandbox. Build everything that doesn't need
                    creds (WA templates approved, DLT SMS submitted, Insurance
                    partner API explored).
Week 4              TBO sandbox arrives. Fill in 4 HTTP endpoints in adapter.
                    GATE A — all endpoints pass sandbox test.
Week 5              Internal cohort dark launch (10 internal accounts). Real
                    bookings, real money, real ticket lifecycle.
                    GATE B — zero P0 issues over 50+ test bookings.
Weeks 6-7           Limited cohort soft launch (5k users, 10% canary).
                    GATE C — p99 < 5s, refund < 7d, conversion within 80%
                    of Duffel benchmark, zero settlement mismatch.
Weeks 8-9           50% canary.
                    GATE D — same 4 metrics + ticket volume per 1k bookings < 3.
Week 10             100% full launch + PR push + wallet ramp to ₹1L.
Week 12             Renegotiate margin terms. Add Phase 2 verticals.
```

**Rollback triggers anywhere:** settlement mismatch > ₹50k, refund SLA breach > 5%, P0 ticket volume > 10/day.

### Tree 2 — Provider routing logic (country-aware)

```
search(origin, destination, date, pax) →

  geography classification by country pair:
    (IN, IN)        → TBO primary + Amadeus failover, skip Duffel (saves 3s)
    (IN, *)/(*, IN) → TBO + Duffel parallel, dedupe by airline+flight#
    (*, *) non-IN   → Duffel primary + Amadeus failover

  timeout strategy:
    primary up to 3s; fire fallback in parallel at 3s; merge if both
    return within 5s; whoever responds first wins

  deduplication:
    same airline + flight + date → ONE card, cheapest price across providers,
    internal `fulfilled_by` tag for booking routing

  caching:
    5-min within-session cache (origin, dest, date, pax, cabin)

  empty handling:
    primary returns 0 + fallback returns N → silently use fallback (no
    "we failed over" message); all providers 0 → suggest nearby dates/airports

  hide provider name from end user (industry standard)
```

### Tree 3 — Universal Trip ID (cancel/refund propagation)

```
Trip lives in booking-service (V44 migration).
Owner: extends booking-service rather than spawning a 12th microservice.

Schema:
  trips      (id, user_id, name, origin/dest cities + countries + states,
              start/end dates, intent enum, pax, status, bundle_discount_paise)
  trip_legs  (id, trip_id FK, leg_type, external_booking_id, external_service,
              status, leg_order, amount, refund tracking)
  Unique index on (external_service, external_booking_id) → same booking
  cannot be attached to two trips.

Cancel propagation:
  cancel ONE leg  → mark cancelled; Trip → PARTIAL_CANCEL; nudge user
                    "want to cancel siblings?"; do NOT auto-cascade
  cancel WHOLE trip → confirmation modal showing per-leg refund estimates;
                    cascade-cancel each leg via its respective service
  airline-initiated → same flow as user-initiated; suggest auto-rebook

Refund:
  Each leg refunds independently per provider's fare rule.
  Bundle discount NOT clawed back on partial cancel (simpler ops + customer-friendly).

Trip Intent:
  Inferred at creation from route + dates + group + user profile.
  User can override; override logged for ML training.
```

### Tree 4 — Refund auto-confirm thresholds

```
Cancellation request →

  source classification:
    user-initiated      → run threshold matrix below
    airline-initiated   → FAST-TRACK 100% refund (skip thresholds)
    admin-initiated     → skip thresholds (already approved)

  user-initiated × amount × fare-rule matrix:
    ≤ ₹10k    + REFUNDABLE → AUTO-CONFIRM
    ₹10k-50k  + REFUNDABLE → ADMIN review (24h SLA)
    > ₹50k    + REFUNDABLE → SR ADMIN (4h SLA, 2-person sign-off)
    intl OR group(>4)      → SR ADMIN (4h)
    PARTIAL                → ADMIN (regardless of amount)
    NON-REFUNDABLE         → REJECT + offer flight credit/voucher
                             (preserves LTV; never hard-refuse)

  customer "I dispute this" click → ALWAYS routes to admin queue
  past-dispute history ≥ 3 in 6 months → flag for L2 review

  settlement-side balance check:
    TBO settled?  → trigger refund cleanly
    not yet?      → pay-from-Razorpay direct + reconcile with TBO later
                    (track receivable; switch to "wait" if float > ₹5L)

  comms cadence:
    T+0       "Refund initiated for ₹X. Expected: 5-7 days."
    T+24h     "Refund processing. Status: [provider state]."
    T+5d      "Still pending? Reply for help." (if not yet completed)
    T+complete "Refund of ₹X credited to card ending XXXX."
```

### Tree 5 — Trip Intent rule table

```
Storage: DB table `trip_intent_rules` (not hardcoded Java)
  → ops adds Sabarimala or new festival window without a deploy

Schema:
  rule_name, priority (lower=higher), trigger_type (DESTINATION/ROUTE/DATE
  /GROUP/HISTORY/COMPOUND), trigger_value (JSONB), inferred_intent,
  suggested_verticals[], vertical_filters (JSONB), applies_to_country[]
  (default ['IN']), enabled, audit fields

Seed rules (priority bands):
  10  PILGRIMAGE:  TIR/IXM/IXR/VNS/TRV/SAG → STAY (veg-only) + COOK (sattvik)
                                              + PANDIT + CAB
  20  DATE-CONTEXT: Diwali week + home-city → FAMILY + festival cook + sweets
                    Christmas/NY + leisure dest → LEISURE_PREMIUM (premium stay
                                                  + spa + experiences)
                    Wedding-season + group ≥ 4 → WEDDING bundle (apartment +
                                                  cook + decor + pandit)
  30  ROUTE/CORRIDOR: BLR↔HYD↔PUN↔CHN↔BOM, 1-pax, ≤2d → BUSINESS
                                                          (hotel-near-airport + cab)
                       outbound → home_city, ≥4d → FAMILY (cab only)
                       any → metro on Fri-Sun → LEISURE_WEEKEND
  35  MEDICAL: Apollo/Manipal/Hinduja/AIIMS-area + medical_history
                → MEDICAL service + apartment-near-hospital + dietary cook
  40  GROUP: 2-pax couple-aged + leisure → LEISURE_COUPLE (premium + spa)
              4+ pax mixed-age + non-leisure → WEDDING_OR_FAMILY_EVENT
  99  FALLBACK: no match → UNCLASSIFIED → STAY only

Conflict resolution:
  Lowest priority wins.
  Within same priority: MOST-SPECIFIC wins (ROUTE > DEST > DATE > GROUP > HISTORY).
  Multi-match same priority → UNION of suggested_verticals.

User override: confirmation page "We think this is [PILGRIMAGE]. Wrong? Tap."
  Override always wins; logged for ML rule-tuning. 30%+ override on a rule = bad rule.

Vertical-availability filter:
  Even if rule says PILGRIMAGE → [STAY, COOK, PANDIT, CAB], on Day 1 PANDIT
  is Phase 2 → silently filter. Rule table doesn't need re-editing when
  Phase 2 ships.

Engine location:
  Day 1: booking-service (Trip lives here, natural fit).
  Phase 3: migrate to ai-service (Python FastAPI) when ML Trip DNA ships.
```

---

## 4. Risk Register (31 failure modes from Reverse Brainstorming)

Each failure mode is mitigated by a specific design or operational choice. Ranked by combined impact × likelihood.

| # | Risk | Severity | Likelihood | Mitigation |
|---|---|---|---|---|
| **1** | TBO settlement delay > 14d → Razorpay refund window expires | HIGH | MED | Daily settlement recon; pay-from-Razorpay direct on unsettled (Tree 4 D4) |
| **2** | TBO wallet runs dry mid-burst | HIGH | MED | Auto-alert at 30% balance; auto top-up rules; cap daily burn |
| **6** | TBO API uptime ~98% (rumored) | HIGH | HIGH | Amadeus failover (Tree 2); 3+2s timeout split; full instrumentation |
| **8** | Webhook signature changes silently | HIGH | LOW | Signature version monitoring; daily webhook-event volume alert |
| **11** | Refund auto-confirm bug (paise vs rupee) | CRITICAL | LOW | ₹10k auto-confirm cap; daily refund-volume alert; unit tests on amount math |
| **17** | WhatsApp templates de-listed by Meta | HIGH | MED | Template diversification; SMS fallback per channel logic; opt-in clarity |
| **20** | Trip Intent wrongly culturally classifies → viral PR | CRITICAL | LOW | Conservative defaults; user override visible Day 1; route-only inference |
| **22** | TBO signs exclusive with competitor | HIGH | LOW | Multi-provider strategy; TripJack + TravClan as backups (drafted) |
| **28** | Analysis paralysis on choosing TBO vs TripJack vs TravClan | HIGH | HIGH | Pre-committed: TBO is primary if it lands first; others = adapters |
| **31** | **Single-country lock-in** | HIGH | HIGH | **MITIGATED Day 1**: V45 migration adds country/state/region; routing logic country-aware |
| 3 | Margin model thinner than projected | MED | MED | Dynamic markup (Param 1); month-3 renegotiation built into rollout (Tree 1) |
| 4 | GST audit flags TBO-as-agent setup | HIGH | LOW | Legal review at month 3; switch to Safar-as-agent if volume justifies |
| 5 | TBO unilateral rate hike | MED | MED | TripJack/TravClan as backup adapters; multi-provider switching |
| 7 | Session token re-auth storm | MED | LOW | Synchronized refresh + 1hr buffer (already in adapter scaffold) |
| 9 | Provider-routing bug → wrong provider | MED | LOW | Unit test coverage on routing logic; canary rollout (Tree 1) |
| 10 | Universal Trip ID race condition | HIGH | LOW | Unique index on (external_service, external_booking_id) (V44) |
| 12 | Admin queue piles up | MED | MED | SLA monitoring + auto-escalation (Tree 4); ops headcount aligned to volume |
| 13 | Customer disputes spike to 10% | HIGH | MED | Conservative auto-confirm threshold; clear fare-rule display upfront |
| 14 | Webhook + manual cancel = conflicting state | LOW | MED | Last-write-wins with audit trail; admin reconciliation UI |
| 15 | Partial-cancel propagation bug | HIGH | LOW | Integration tests; never auto-cascade per Tree 3 |
| 16 | 5s timeout returns 0 results during TBO peak | HIGH | MED | Geography routing minimizes (Tree 2); Amadeus failover |
| 18 | EN+HI translation: ₹50k vs ₹500 | CRITICAL | LOW | Centralized money-formatting utility; locale-aware tests |
| 19 | Cross-vertical hub drowns confirmation | MED | MED | Hub on separate page tab, not inline; A/B test layout |
| 21 | TBO sales team poaches our customers | MED | MED | Contract clauses (no marketing to our customers); minimize PII shared |
| 23 | DGCA changes ticketing rules | LOW | LOW | Legal monitoring; multi-provider strategy gives optionality |
| 24 | Customers learn we're TBO-fronted | LOW | MED | Cross-vertical bundle is the moat — fares alone aren't differentiator |
| 25 | Bundle discount cannibalises margin | MED | MED | Phase 1 = static 8-12%; A/B vs no-discount; dynamic in Phase 2 if needed |
| 26 | Canary novelty effect → wrong extrapolation | MED | MED | Compare to control group on existing flight flow; longer canary if uncertain |
| 27 | PR launch + TBO outage simultaneously | HIGH | LOW | Soft-launch first (no PR); only press-push after 2 weeks of stability |
| 29 | Cohort pilot biased baseline | MED | HIGH | Mix in non-loyal users to cohort; validate on 50% canary before 100% |
| 30 | MMT launches PG+flight bundle 60d after | HIGH | MED | Speed-to-market is the answer; 90-day plan moves fast |

---

## 5. Multi-Country Readiness

### Implemented Day 1 (this commit cycle)

- Trip schema: `origin_country`, `destination_country`, `origin_state`, `destination_state`, `origin_region`, `destination_region` (V45 migration)
- Trip entity: country fields default to 'IN', NOT NULL going forward
- Provider routing logic: country-pair-based (Tree 2)
- Trip Intent rules: `applies_to_country[]` field on rule table

### Deferred to dedicated "Multi-country readiness" sprint

This is a separate, larger effort that should be scheduled before any actual country expansion is committed:

- `listings.listings`, `users.users`, `users.addresses`, `builders.builder_projects`, `sale.properties`, `chef.chef_profiles`, `partner_vendor` — all need country field
- Phone numbers — currently `+91` hardcoded; needs country-code abstraction
- Currency — currently INR/paise hardcoded; needs multi-currency Money type
- GST → generic tax abstraction
- Aadhaar/PAN → KYC abstraction (passport, Emirates ID, etc.)
- i18n — currently EN/HI; needs locale framework
- Hardcoded "India" / 'IN' / Indian-airport-set sets across services

**Recommendation:** Run this as its own BMAD brainstorm before committing to any specific country. ROI of doing this when the codebase is small is enormous; ROI after 50+ services have hardcoded assumptions is brutal.

---

## 6. Implementation Roadmap

### Week 0 — Foundation (DONE, commit ea209b3 + V45)
- [x] TBOAirFlightAdapter scaffold (5 TODO blocks documenting full TBO API contract)
- [x] tboWebClient bean + tbo: config block (master switch OFF by default)
- [x] V44 migration: Universal Trip + TripLeg tables
- [x] V45 migration: country/state/region on Trip
- [x] 4 enums: TripIntent, TripStatus, LegType, LegStatus
- [x] TripRepository, TripLegRepository

### Weeks 1-3 — Pre-cred prep (no TBO creds needed)
- [ ] User: send 3 partner emails (TBO + TripJack + TravClan) — drafts in `docs/flight-aggregator-partner-outreach.md`
- [ ] WhatsApp templates submitted to MSG91 for approval (3 templates: confirmed/cancelled/checkin)
- [ ] DLT SMS template registration submitted
- [ ] Insurance partner API explored (Acko or ICICI Lombard travel-insurance)
- [ ] TripService: CRUD + leg-attach API + cancel-leg/cancel-trip orchestration
- [ ] V46 migration: trip_intent_rules table + 40-rule seed
- [ ] Cross-vertical suggestion engine: rule evaluator service
- [ ] "Complete your trip" hub UI scaffold on safar-web confirmation page

### Week 4 — TBO sandbox arrives
- [ ] Fill 5 TODO blocks in TBOAirFlightAdapter (~3-4 hours)
- [ ] End-to-end sandbox test: search → fareQuote → book → cancel → refund
- [ ] Webhook config + signature verification
- [ ] Set TBO_ENABLED=true in dev env
- [ ] **GATE A** — all endpoints pass sandbox test

### Week 5 — Internal cohort dark launch
- [ ] Feature flag: TBO live for 10 internal Safar accounts
- [ ] Real bookings with real money
- [ ] Cross-vertical bundle live (with synthetic stays/cooks for testing)
- [ ] **GATE B** — zero P0 issues over 50+ test bookings

### Weeks 6-7 — Limited cohort soft launch (10% canary)
- [ ] Open to existing PG tenants + past hotel bookers (~5k users)
- [ ] Daily reconciliation job running
- [ ] **GATE C** — p99 < 5s, refund < 7d, conversion within 80% of Duffel, zero settlement mismatch over 7 days

### Weeks 8-9 — 50% canary
- [ ] Ramp to 50% of India-route searches
- [ ] **GATE D** — same 4 metrics + ticket volume per 1k bookings < 3

### Week 10 — Full launch
- [ ] 100% of India routes
- [ ] PR push, home-page feature, marketing campaigns
- [ ] Wallet ramped from ₹50k to ₹1L

### Week 12 — Optimize + extend
- [ ] Renegotiate margin terms with TBO (volume justifies)
- [ ] Push for credit-line + bank-guarantee model
- [ ] Add Phase 2 verticals: Pandit + Decor + Spa + Experiences
- [ ] Begin ML Trip DNA model design (Phase 3)

---

## 7. Open Decisions / Dependencies

| # | Item | Owner | Blocked by |
|---|---|---|---|
| 1 | Send TBO + TripJack + TravClan partner emails | Sawan | Nothing (drafts ready in `docs/flight-aggregator-partner-outreach.md`) |
| 2 | Sign up at Duffel.com → get production token | Sawan | Nothing (5-min self-serve) |
| 3 | TBO sandbox creds → fill 5 TODO blocks | Engineering | TBO partnership signing |
| 4 | MSG91 DLT registration for SMS | Operations | Nothing (regulatory paperwork) |
| 5 | Insurance partner selection | Product | Sandbox eval of Acko vs ICICI Lombard |
| 6 | Multi-country readiness sprint scheduling | Leadership | Strategic decision on first expansion country |
| 7 | Trees 4-5 of brainstorm formally accepted | Sawan | This doc serves as record |

---

## 8. Reference Material

- **Brainstorm session:** `_bmad/docs/brainstorming/brainstorming-session-2026-04-26-tbo-design-plan.md`
- **Partner outreach drafts:** `docs/flight-aggregator-partner-outreach.md`
- **Existing flight architecture:** `services/flight-service/src/main/java/com/safar/flight/`
- **Universal Trip schema:** `services/booking-service/src/main/resources/db/migration/V44__universal_trip.sql` + `V45__trip_country_state_region.sql`
- **TBO public docs:** `dealint.tboair.com` (login required after partnership)
- **Related session memory:**
  - `session-2026-04-26.md` — Duffel pivot (morning)
  - `session-2026-04-26-sprint2.md` — Flight next-tier impl
  - `session-2026-04-26-sprint4.md` — TBO scaffold + Universal Trip schema
