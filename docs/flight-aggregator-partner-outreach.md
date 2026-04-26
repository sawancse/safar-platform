# Flight Aggregator Partner Outreach — Apr 2026

**Strategy:** Apply to all three in parallel. Whichever responds fastest with usable terms wins first integration. See `session-2026-04-26.md` for full research backing this decision.

**Final stack target (Month 3):**
- **Duffel** = international + NDC fallback (already shipped, awaiting access token)
- **TripJack** OR **TBO Air** = Indian LCC primary
- **TravClan** = backup + experimental terms / negotiation leverage

---

## Before sending — placeholders to replace

Search for `[BRACKETS]` in the emails below and fill in:
- `[Name]` — your name
- `[Title]` — your title (Founder / Co-founder / CEO)
- `[Safar Platform Pvt Ltd]` — exact legal entity
- `[mobile]` — best phone for callback (with `+91` country code)
- `[email]` — your business email (the one you're sending from)
- `[website]` — public site (ysafar.com? safar.com?)
- `[N]` cities — current operational footprint (be conservative; under-promise is fine)
- `[200–500 bookings/mo]` — adjust if you want a different ballpark

---

## Email 1 — TravClan (send first)

**To:** `hello@travclan.com`
**Also try:** Chirag Agrawal LinkedIn DM (founder), form at `https://home.travclan.com/api-b2b-flights-hotels-packages-travclan/`
**Subject:** Safar ↔ TravClan — quick chat about a flight integration?

---

Hi [Chirag / TravClan team],

[Name] here, founder of Safar — a multi-vertical India marketplace (PG / hotel / apartment / cook / builder / donations / services) that's adding flights as a native vertical.

Reaching out because we like how you've structured your flight API offering — modern REST stack, multi-source aggregation, and (frankly) a startup ergonomics that the bigger consolidators don't match. We've already wired Amadeus and Duffel via a clean adapter pattern, so adding a TravClan adapter is days, not weeks.

What I'd like to discuss:
1. Sandbox + docs to validate the integration shape
2. Commercials — your published B2B model is more startup-friendly than TripJack/TBO; want to understand how it scales as volume grows
3. Whether TravClan would be open to a co-pilot-style partnership early — we route Indian LCC traffic to you, you give us inventory + favorable settlement terms while we both grow

Initial volume realistic for first quarter is [200–500 bookings/mo], with the natural ceiling determined by our PG tenant base (currently [N] active tenants).

Open to a 20-min call this week if useful. Or reply with whatever info you need from us first.

Cheers,
[Name]
[Safar Platform Pvt Ltd] · [mobile] · [website]

---

## Email 2 — TripJack (send same day)

**To:** `partners@tripjack.com`
**Also try:** `business@tripjack.com`, form at `https://tripjack.com/nav/b2b-portal-for-travel-agents`
**Subject:** Safar (multi-vertical India marketplace) — flight API partner enquiry

---

Hi TripJack team,

I'm [Name], founder of Safar — an India-focused marketplace that today serves PG tenants, hotel/apartment guests, builders' buyers, religious-trip planners and more. We're now adding flights as a native vertical so customers can book travel + stay + services in one trip.

We've evaluated several flight aggregators and TripJack is our top pick for Indian inventory because of your full LCC coverage (IndiGo, Air India, Vistara, SpiceJet, Akasa, AirAsia) and the depth of refund / re-issue tooling you provide.

**A bit about our fit:**
- Existing user base across PG/stay/services across [N] Indian cities (Hyderabad, Bengaluru, Pune, Chennai concentrated)
- Our flight integration is already production-shaped (Spring Boot adapter pattern, Razorpay collection, Kafka events for confirmations / cancellations) — we can integrate fast once sandbox is open
- Initial monthly volume estimate: [200–500 bookings, scaling with PG-renewal cohort]
- Indian PG tenant base creates repeat home-route demand which is naturally aligned with your sleeper / Tatkal inventory strength

**What we'd like:**
1. Sandbox API access + technical docs
2. A 30-min intro call to discuss the MSA, prepaid wallet sizing, and onboarding timeline
3. Name of an account manager we should keep in the loop

I'm happy to share a deck or do the call this week. Reply to this email or [mobile] / WhatsApp.

Best,
[Name]
[Title], [Safar Platform Pvt Ltd]
[mobile] · [website]

---

## Email 3 — TBO Air (send same day)

**To:** `apisupport@tboair.com`
**Also try:** registration form at `https://apiintegration.tboholidays.com`
**Subject:** API integration enquiry — Safar (India marketplace, REST/JSON, ready to integrate)

---

Dear TBO Air Team,

I'm writing on behalf of Safar Platform — a multi-vertical Indian marketplace covering property rentals, PG accommodation, hotels, services and (now) flights. We're evaluating long-term flight content partners and TBO Air is on our shortlist on account of the technical maturity of your REST/JSON product line and the institutional stability TBO offers as an NSE-listed group.

**Brief context on our integration readiness:**
- Java 17 / Spring Boot 3 microservices architecture; flight-service already structured around a pluggable provider-adapter pattern (Amadeus already wired, Duffel sandbox in test). Adding a TBOAir adapter is straightforward once docs + sandbox creds are issued.
- Payments via Razorpay; settlement via prepaid wallet acceptable.
- Kafka-driven event pipeline for booking lifecycle (confirmed / cancelled / refunded) already operational.
- Footprint: [N] Indian cities, primarily Tier 1 + emerging Tier 2.

**We would appreciate:**
1. Information on the agent onboarding process (KYC, deposit / bank-guarantee structure, expected timelines)
2. Sandbox API credentials and current API documentation (REST/JSON product, https://dealint.tboair.com)
3. An introductory call with your business development team to discuss commercials and SLA

Please let me know who the right contact is. I am available for a call any working day this or next week.

Sincerely,
[Name]
[Title], [Safar Platform Pvt Ltd]
[mobile] · [email] · [website]

---

## Response tracker

Update this section as replies come in.

| Partner | Sent date | First response | Sandbox creds | MSA shared | Live | Notes |
|---|---|---|---|---|---|---|
| TravClan | _ | _ | _ | _ | _ | _ |
| TripJack | _ | _ | _ | _ | _ | _ |
| TBO Air | _ | _ | _ | _ | _ | _ |

---

## Follow-up cadence

- **Day 0:** Send all three (today)
- **Day 4 (no reply):** Polite nudge — "Wanted to make sure my email didn't get buried; happy to share more context if useful."
- **Day 10 (no reply):** Try a different channel (LinkedIn DM to a sales/partnership lead, WhatsApp to listed business number, founder DM for TravClan)
- **Day 21 (still no reply):** Consider as effectively no — focus on the responsive ones

When **any** reply lands: respond within 4 hours with a 30-min calendar link. Indian B2B sales velocity is set by your responsiveness, not theirs.

---

## What "ready to integrate" looks like on our side

When sandbox creds arrive for any provider, the integration path is:
1. Add `{Provider}FlightAdapter implements FlightProviderAdapter` (~3-4 hours, follows DuffelFlightAdapter pattern at `services/flight-service/src/main/java/com/safar/flight/adapter/duffel/`)
2. Add `{provider}WebClient` bean to `WebClientConfig`
3. Add `{provider}:` block to `application.yml`
4. Add provider to `FlightProvider` enum
5. Sandbox testing: search → book → cancel/refund full loop
6. Set `FLIGHT_PRIMARY_PROVIDER={PROVIDER}` to switch primary booker

Total time per new adapter: 1-2 days end-to-end.
