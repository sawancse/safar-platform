---
stepsCompleted: [1, 2]
inputDocuments: []
session_topic: 'Flight booking feature for Safar platform — domestic + international, API aggregator model'
session_goals: 'Tech stack & API decisions, revenue model, UX innovation, cross-sell with stays/cooks/PG/builders, competitive differentiation, go-to-market'
selected_approach: 'ai-recommended'
techniques_used: ['cross-pollination', 'morphological-analysis', 'chaos-engineering']
ideas_generated: []
context_file: ''
---

# Brainstorming Session: Flight Booking for Safar

**Date:** 2026-04-17

## Session Overview

**Topic:** Adding flight booking (domestic + international) to Safar platform using API aggregator model
**Goals:** Tech architecture, revenue model, UX, cross-sell, competitive differentiation, GTM strategy
**Special Interest:** MakeMyTrip-level capabilities, TBO/Duffel API integration

### Session Setup

- Safar is an India-first travel super-app with stays, PG, cooks, builders
- Flight booking is the next major feature to become a full travel platform
- API aggregator approach confirmed (TBO Air for India, Duffel for international)
- MakeMyTrip tech stack analyzed as reference architecture

## Technique Selection

**Approach:** AI-Recommended Techniques
**Analysis Context:** Complex multi-domain product feature spanning tech, business, UX, and competitive strategy

**Recommended Techniques:**

- **Cross-Pollination:** Transfer patterns from MakeMyTrip, Booking.com, Skyscanner, Uber, Swiggy to flight booking
- **Morphological Analysis:** Systematic parameter combination matrix (API x Revenue x UX x Market x Cross-sell)
- **Chaos Engineering:** Stress-test the architecture against failure scenarios before committing

**AI Rationale:** Flight booking is a complex, multi-parameter problem that benefits from borrowing proven patterns (Phase 1), systematically exploring all combinations (Phase 2), and stress-testing against failures (Phase 3).

## Technique Execution Results — Cross-Pollination

### Ideas Generated (9)

**[Cross-Poll #1]: Live Trip Pulse**
_Concept:_ After booking, app becomes a living trip timeline — countdown, check-in reminders, gate updates, cross-sell nudges for cooks/stays at destination. Like Swiggy order tracking for your entire trip.
_Novelty:_ No Indian OTA does trip-as-a-living-entity. MakeMyTrip shows a dead confirmation page.

**[Cross-Poll #2]: Trip Upsell Windows (Swiggy "Add Items")**
_Concept:_ Time-sensitive upsell windows across trip lifecycle — airport pickup 5 days out, lounge access 3 days out, meal upgrade at check-in, cook add-on after landing. Drip revenue funnel tied to urgency and context.
_Novelty:_ MakeMyTrip upsells at booking time only. This creates lifecycle revenue.

**[Cross-Poll #3]: "Your Travel DNA" (Spotify Discovery)**
_Concept:_ Algorithmic route discovery based on cross-platform booking history (stays + cooks + PG + flights). "You love coastal + food cities → direct flight to Kochi this weekend + Kerala seafood cook."
_Novelty:_ Multi-vertical signal that no pure flight OTA has. Safar's super-app moat.

**[Cross-Poll #4]: PG Tenant "Home Route" Auto-Pilot**
_Concept:_ Detect PG tenant's hometown from profile, auto-create recurring flight watch (DEL↔BLR every 3rd weekend). Learn preferences over time — airline, timing, seat. One-tap booking.
_Novelty:_ Impossible for MakeMyTrip — they don't have PG/tenancy data. Safar-only moat.

**[Cross-Poll #5]: PG Rent + Flight Subscription Bundle**
_Concept:_ Bundle monthly home flight into PG rent auto-debit via existing Razorpay subscription. "INR 8,000 rent + INR 3,200 flight = INR 11,200/month." Like a phone plan for rent + travel.
_Novelty:_ Nobody bundles accommodation subscription with recurring flight. Massive lock-in.

**[Cross-Poll #6]: WhatsApp-First Flight Booking**
_Concept:_ Book flights via WhatsApp — "flight DEL BOM 25 May" → 3 options → reply '1' → UPI link → e-ticket PDF. No app download. Captures tier-2/3 India that won't install another travel app.
_Novelty:_ Targets 400M+ WhatsApp-native Indians that MakeMyTrip's heavy app can't reach.

**[Cross-Poll #7]: Group Trip Chat → Auto Flight Sync**
_Concept:_ Trip group chat — someone shares a flight, others tap "Match this flight" from their cities. Auto-syncs arrival times across DEL/BLR/HYD origins. Solves multi-city group coordination nightmare.
_Novelty:_ Every OTA forces individual searches. Safar solves group trip coordination.

**[Cross-Poll #8]: "Flight Roulette" — Mystery Fare Game**
_Concept:_ Set budget + dates, spin the wheel, get a mystery destination. Creates virality — "I spun INR 2,500 and got Andaman!" Targets 20-30 adventure demographic.
_Novelty:_ No Indian OTA gamifies flight discovery. Social media virality engine.

**[Cross-Poll #9]: Safar Miles Cross-Vertical Flywheel**
_Concept:_ Flights earn Safar Miles redeemable across PG rent, cook bookings, stays. "Double Miles Weekends" like Dream11 mega contests. Cross-vertical loyalty loop no competitor can match.
_Novelty:_ MakeMyTrip loyalty is flight-only. Safar Miles span 4+ verticals = stickier ecosystem.

### Session Highlights

**Breakthrough Insight:** PG tenancy data creates a flight booking moat no competitor can replicate — recurring home route detection, rent+flight bundles, cross-vertical loyalty.
**Key Theme:** Safar's super-app advantage = multi-vertical data signals that pure OTAs don't have.
**Strongest Ideas:** #4 (Home Route Auto-Pilot), #5 (Rent+Flight Bundle), #9 (Miles Flywheel)
