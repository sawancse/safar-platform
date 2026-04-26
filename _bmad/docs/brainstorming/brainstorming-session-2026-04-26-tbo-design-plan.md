---
stepsCompleted: [1, 2, 3, 4]
inputDocuments: []
session_topic: 'TBO Air integration — design and rollout plan'
session_goals: 'Produce (1) a TBO Air integration architecture/adapter design, (2) a rollout plan from sandbox → cutover including coexistence with Duffel/Amadeus, (3) a risks + dependencies map across commercial, technical, GST/settlement, and operational dimensions'
selected_approach: 'ai-recommended'
techniques_used: ['Morphological Analysis', 'Decision Tree Mapping', 'Reverse Brainstorming']
ideas_generated: 50
session_status: 'COMPLETE — all 3 phases done. Synthesis written to docs/tbo-integration-design.md'
completed_at: '2026-04-26'
---

# Brainstorming Session Results

**Facilitator:** Sawan
**Date:** 2026-04-26

## Session Overview

**Topic:** TBO Air integration — design and rollout plan
**Goals:** Produce (1) a TBO integration architecture, (2) a rollout plan from sandbox → cutover with coexistence vs Duffel/Amadeus, (3) a risks + dependencies map covering commercial / technical / GST / operational dimensions

### Session Setup

User picked AI-Recommended technique sequence: Morphological Analysis → Decision Tree Mapping → Reverse Brainstorming. After Tree 3 user pivoted to implementation ("BUILD NOW") so Trees 4-5 deferred. Output captured below covers the full Phase 1 (8-parameter design space) + 3 of 5 decision trees in Phase 2.

## Phase 1 — Morphological Matrix (LOCKED, all 8 parameters)

| # | Category | Decision |
|---|---|---|
| 1 | Commercial | ₹50k → ₹2L wallet ramp, prepaid → credit, dynamic markup (₹150-250 full-service / ₹75 LCC), TBO as agent of record for GST |
| 2 | Technical adapter | Full lifecycle Day 1, TBO India + Duffel intl + Amadeus failover, cached session auth, hybrid retry (3× transient) |
| 3 | Rollout | Canary 10/50/100 over 3 weeks, cohort pilot first (5k existing PG/hotel users), dark→soft→full launch |
| 4 | Operational | Auto-refund ≤ ₹10k, push+poll for changes, hybrid support, mirrored PNR + daily recon |
| 5 | Risk/monitoring | Daily recon (financial + status), full instrumentation, Amadeus failover when TBO down, full request/response logging Day 1 |
| 6 | Customer support / ticketing | Extend PG ticket system (S26); 4 SLA tiers (4h/24h/48h/72h); 3-level escalation L1 ops → L2 supervisor + TBO L1 → L3 TBO account manager + airline desk |
| 7 | Notifications | MSG91 WA + SMS + email + push + in-app; per-event channel mix; EN+HI Day 1; smart dedupe within 1hr |
| 8 | Cross-vertical service suggestion engine | Confirmation + 2-day-before-departure trigger; rule-based logic Day 1 → ML in Phase 3; "Complete your trip" hub; Universal Trip ID built upfront; 8-12% bundle discount; **Day 1 verticals = Stay + Cab + Cook + Insurance** (Phase 2 = Pandit + Decor + Spa + Experiences) |

## Phase 2 — Decision Trees (Trees 1-3 done, Trees 4-5 deferred)

### Tree 1 ✅ 90-day rollout sequence
- Week 0: Send partner emails + build adapter scaffold + Trip schema + suggestion rules
- Weeks 1-3: Wait for TBO sandbox; build everything that doesn't need creds
- Week 4: Sandbox arrives → fill in 4 HTTP endpoints → end-to-end test (GATE A)
- Week 5: Internal cohort dark launch (10 internal accounts) (GATE B)
- Weeks 6-7: Soft launch limited cohort (5k users, 10% canary) (GATE C: p99<5s, refund<7d, conversion within 80% of Duffel, zero settlement mismatch)
- Weeks 8-9: 50% canary (GATE D)
- Week 10: 100% full launch + PR push, wallet ramped to ₹1L
- Week 12: Renegotiate margin terms + add Phase 2 verticals
- Rollback triggers: settlement mismatch >₹50k, refund SLA breach >5%, P0 ticket volume >10/day

### Tree 2 ✅ Provider routing logic
- Domestic India route: TBO primary, Amadeus failover, Duffel skipped
- India outbound/inbound: TBO + Duffel parallel, dedupe by airline+flight#
- Pure international: Duffel primary, Amadeus failover
- 5s timeout (3s primary + 2s extension to fallback)
- Dedupe: same airline + flight + date → one card showing cheapest, internal `fulfilled_by` tag for booking routing
- Hide provider name from end user (industry standard)
- 5-min within-session cache

### Tree 3 ✅ Universal Trip ID (schema + cancel/refund propagation)
- Owner: extend `booking-service` (don't create 12th microservice)
- Schema: `trips` (id, user_id, name, origin/dest cities, dates, intent enum, pax, status, bundle_discount_paise) + `trip_legs` join (trip_id, leg_type, external_booking_id, status, leg_order)
- Cancel propagation: cancel-one-leg = mark leg cancelled + Trip → PARTIAL_CANCEL + nudge user; cancel-trip = cascade with confirmation modal showing per-leg refund estimates; airline-initiated cancel via webhook = same flow
- Bundle discount NOT clawed back on partial cancel (simpler ops + customer-friendly)
- Each leg refunds independently per its provider's fare rule
- Trip Intent inferred from route + dates + group composition + user profile; user can override

### Tree 4 ✅ Refund auto-confirm threshold + escalation
- Airline-initiated cancels (TBO/Duffel webhook) → fast-track 100% refund (skip thresholds)
- Admin-initiated → skip thresholds (admin already approved)
- User-initiated × amount × fare-rule matrix:
  - ≤ ₹10k REFUNDABLE → AUTO-CONFIRM
  - ₹10k-50k OR PARTIAL → ADMIN (24h SLA)
  - >₹50k OR international OR group(>4) → SR ADMIN (4h, 2-person sign-off)
  - NON-REFUNDABLE → REJECT + offer flight credit/voucher (preserve LTV)
- Customer "I dispute this" click → ALWAYS routes to admin queue
- Past-dispute history ≥ 3 in 6 months → flag for L2 review
- Settlement-side: if TBO unsettled, pay-from-Razorpay direct + reconcile later (customer experience > our float, until float >₹5L)
- Notification cadence: T+0, T+24h, T+5d (if pending), T+complete

### Tree 5 ✅ Trip Intent rule table (destination → vertical mapping)
- Rule storage: DB table `trip_intent_rules` (not hardcoded Java) — ops adds new rules without deploy
- Schema: id, rule_name, priority (lower=higher), trigger_type (DESTINATION/ROUTE/DATE/GROUP/HISTORY/COMPOUND), trigger_value (JSONB), inferred_intent, suggested_verticals[], vertical_filters (JSONB), enabled, audit fields
- Seed rules across 6 categories:
  - PILGRIMAGE (priority 10): TIR/IXM/IXR/VNS/TRV/SAG → STAY (veg-only) + COOK (sattvik) + PANDIT + CAB
  - DATE-CONTEXT (priority 20): Diwali week + home-city → FAMILY + festival cook + sweets vendor; Christmas/NY + leisure dest → LEISURE_PREMIUM; wedding-season + group ≥ 4 → WEDDING bundle
  - ROUTE/CORRIDOR (priority 30): IT corridor (BLR↔HYD↔PUN↔CHN↔BOM, 1-pax, ≤2d) → BUSINESS; outbound to home_city ≥4d → FAMILY; metro on Fri-Sun → LEISURE_WEEKEND
  - GROUP (priority 40): couple-aged 2-pax + leisure → LEISURE_COUPLE (premium stay+spa); 4+ pax mixed-age + non-leisure → WEDDING_OR_FAMILY_EVENT
  - MEDICAL (priority 35): Apollo/Manipal/Hinduja/AIIMS-area + medical_history flag → MEDICAL service + apartment-near-hospital + dietary-restricted cook
  - FALLBACK (priority 99): no match → UNCLASSIFIED → STAY only
- Conflict resolution: lowest priority wins; within same priority, MOST-SPECIFIC wins (ROUTE > DEST > DATE > GROUP > HISTORY); multi-match same priority → UNION suggested_verticals
- User override: confirmation-page "We think this is a [PILGRIMAGE] trip. Wrong? Tap to change." Override always wins; logged for ML training
- Vertical-availability filter: rule says PANDIT but PANDIT is Phase 2 → silently filter; rule table doesn't need re-editing when Phase 2 ships
- Engine lives in booking-service (Day 1), migrates to ai-service (Python FastAPI) when ML Trip DNA ships in Phase 3

## Phase 3 — Reverse Brainstorming (Failure-mode post-mortem)

### Surfaced 31 failure modes across 6 categories — each becomes a risk in the register

**Commercial fails:**
- #1 TBO settlement delay >14d → Razorpay refund window expires → unrecoverable
- #2 TBO wallet runs dry mid-burst → checkout failures during Diwali → public embarrassment
- #3 Margin model thinner than projected; LCC commission can't cover CAC; flights = permanent loss-leader
- #4 GST audit flags TBO-as-agent as non-compliant → backdated tax + penalties
- #5 TBO unilateral rate hike at 6 months → margin collapse

**Technical fails:**
- #6 TBO API uptime ~98% → 7+hr/mo outage → silent abandonment
- #7 Cached session token expires mid-burst → re-auth storm → rate-limited → 30min outage
- #8 Webhook signature changes silently → cancellation events drop for 3wks → trust collapse
- #9 Provider-routing bug → all India routes go to Duffel (empty) → 0 conversion 4 days
- #10 Universal Trip ID race condition → same booking attached to 2 trips → cancel cascades wrong

**Operational fails:**
- #11 Refund auto-confirm bug fires `amount × 100` → ₹50L wrong refund in 1 day
- #12 Admin queue → 200 unresolved tickets → SLA breach → consumer-court complaints
- #13 Customer disputes spike to 10% → ops can't keep up → 5-day backlog
- #14 Schedule-change webhook + manual cancel = conflicting state in DB
- #15 Partial-cancel propagation bug → user's stay auto-cancelled despite Tree-3 NEVER policy

**Customer experience fails:**
- #16 5s timeout returns 0 results during TBO peak hours → 50% empty searches → bounce spike
- #17 WhatsApp messages flagged spam at scale → MSG91 templates de-listed by Meta → channel lost
- #18 EN+HI translation issue: ₹50,000 vs ₹500 (decimal mismatch) → mass disputes
- #19 Cross-vertical hub drowns confirmation page → users miss booking ref → ops swamped
- #20 Trip Intent wrongly flags Tirupati flight for Christian family → cultural offense → viral PR

**Strategic fails:**
- #21 TBO sales team poaches our high-LTV customers via PNR/contact-info access
- #22 TBO signs exclusive with competitor mid-contract → no leverage
- #23 DGCA changes ticketing rules → IATA required → setup invalidated
- #24 Customers learn we're TBO-fronted → "why not direct?" → race to bottom
- #25 Bundle discount cannibalises margin without driving real attach

**Marketing/launch fails:**
- #26 Canary 10% novelty effect → wrong extrapolation → ramp budget blown
- #27 Launch day TBO outage → "Safar's flight launch flopped" narrative
- #28 All 3 partners reply → analysis paralysis → 6-week delay → competitor first
- #29 Cohort pilot biased baseline (loyal users convert 3x) → wrong PMF signal
- #30 Bigger competitor (MMT) launches PG+flight bundle 60d after us → moat evaporates

**Multi-country fail (added at Phase 3 close):**
- #31 Single-country lock-in: every entity hardcoded to India. UAE/SG/SE-Asia expansion in 2027-28 = multi-month migration. Adding country/state/region NOW costs minutes; later costs months.

## Multi-Country Design Constraint (added Phase 3)

Per user input, design upgraded to support ISO-3166-1 country codes Day 1:
- Trip schema gets `origin_country`, `destination_country`, `origin_state`, `destination_state`, `origin_region`, `destination_region` (V45 migration)
- Provider routing logic uses country pairs, not "isIndianAirport" boolean
- Trip Intent rules get `applies_to_country` field (default `['IN']`)
- Future expansion = config + new adapter, NOT routing rewrite

**Broader platform multi-country prep is a SEPARATE sprint** — covers Listing/User/Builder/Sale/Cook/PartnerVendor/Payment/i18n/KYC/Tax/Phone-format. Should run before any actual country expansion is planned. ROI is enormous now (small codebase); brutal later.

## Synthesis output
Full design doc written to `safar-platform/docs/tbo-integration-design.md` — single hand-offable document anyone on the team can execute from.
