---
stepsCompleted: [1, 2, 3]
inputDocuments: []
session_topic: 'TBO Air integration — design and rollout plan'
session_goals: 'Produce (1) a TBO Air integration architecture/adapter design, (2) a rollout plan from sandbox → cutover including coexistence with Duffel/Amadeus, (3) a risks + dependencies map across commercial, technical, GST/settlement, and operational dimensions'
selected_approach: 'ai-recommended'
techniques_used: ['Morphological Analysis', 'Decision Tree Mapping', 'Reverse Brainstorming']
ideas_generated: 11
session_status: 'paused-mid-phase-2 — user pivoted to implementation. Trees 1, 2, 3 done; Trees 4, 5 deferred. Pick up at Tree 4 (Refund auto-confirm threshold) when implementation pause ends.'
paused_at: '2026-04-26'
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

### Trees 4-5 deferred
- Tree 4: Refund auto-confirm threshold + escalation logic
- Tree 5: Trip Intent rule table (destination → vertical mapping)
