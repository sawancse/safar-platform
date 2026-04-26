---
stepsCompleted: [1, 2, 3, 4]
inputDocuments: []
session_topic: 'Event-services design gap in chef-service: per-service listings, self-onboard + admin approval, and product-link from booking → catalog item booked'
session_goals: '(a) Per-service listing model + self-onboarding flow that minimizes admin review burden; (b) Booking ↔ catalog product link UX so customers see the actual cake/singer/pandit/etc they booked; (c) Domain re-shape — rename chef-service to event-service, define provider-type taxonomy, plan migration; (d) Treat (a)(b)(c) as one connected design — listing-led onboarding is the load-reducing wedge that makes the rest fall in line'
selected_approach: 'ai-recommended'
techniques_used: ['First Principles Thinking', 'Cross-Pollination', 'Morphological Analysis']
ideas_generated: []
context_file: ''
---

# Brainstorming Session Results

**Facilitator:** sawan
**Date:** 2026-04-26

## Session Overview

**Topic:** Event-services design gap in chef-service — per-service listings, self-onboarding, and booking↔product link

**Goals:**
- **(a) Listing-model design** — each provider type (singer, cake-designer, pandit, decorator, waitstaff, appliance-rental, future: photographer/DJ/mehendi/makeup) has its own listing schema, search, discovery, profile page.
- **(b) Booking ↔ catalog product link** — booking dashboard shows the actual product booked (cake photo, singer profile, puja package, decor mood-board) with a deep link back to the listing.
- **(c) Domain re-shape** — rename `chef-service` → `event-service` (or split), define provider-type taxonomy, design migration from `partner_vendors` directory rows to first-class listings.
- **(d) One connected design** — listing-style self-onboarding is the wedge that reduces admin load *and* unblocks (b) and (c).

**Reduce admin load** is the explicit constraint: today every cake baker / singer / pandit is admin-seeded. Tomorrow they should self-onboard like a host onboards a property — fill the form, upload portfolio, submit, admin clicks Approve.

### Session Setup

**Approach:** AI-Recommended Techniques (3-phase)

**Recommended Technique Sequence:**
- **Phase 1 — First Principles Thinking** (creative): strip the chef-service framing; derive irreducible primitives every service-provider listing must have.
- **Phase 2 — Cross-Pollination / Analogical Thinking** (creative): steal validated patterns from Airbnb, Urban Company, Fiverr, Zomato, Etsy, JustDial, Practo for self-onboarding + admin-load + product-link.
- **Phase 3 — Morphological Analysis** (deep): grid of provider-type × listing-concern, populated to spec level — doubles as listing-schema RFC and migration-plan input.

**AI Rationale:** F-P prevents carrying chef-history baggage into the new domain; analogical thinking injects already-solved patterns from comparable platforms (high leverage given the user said "big platform service"); morphological analysis converts insights into a concrete spec that engineering can read off. Sequenced because each phase constrains the next.

## Phase 1 — First Principles Thinking

### Element 1 — Domain shape (zoom-out)

**[Primitive #1]: Three Co-Equal Legs**
*Concept:* Safar = stay + flight + **services**. Services is the third leg, on architectural par with stay (`listing-service`) and flight (`flight-service`).
*Novelty:* Reframes "chef-service" from a cooking app into the services leg of a trip-and-occasion platform. Forces parity with `listing-service` rather than ad-hoc growth.

**[Primitive #2]: Services is an Attachable Layer, Not a Silo**
*Concept:* Services attach to stay/flight bookings (and stand alone for local occasions). Architecturally: cross-link APIs, occasion-context propagation (`stayBookingId`, `flightBookingId`, `occasion`), unified post-booking dashboards.
*Novelty:* Most marketplaces silo verticals. Trip-context-aware services is what makes the bundle defensible.

**[Primitive #3]: Vendor ↔ Listing ↔ Customer**
*Concept:* The services leg connects end users planning an occasion with vendors who provide occasion services through first-class listings (vendor self-creates, admin approves, customer books).
*Novelty:* Today the listing layer is missing — vendors are admin-seeded directory rows. Naming the listing as the bridge makes it the load-bearing artifact and explains why admin load is high.

### Element 2 — Listing primitives (per-listing data)

**[Primitive #4]: Vendor Identity & KYC** — Legal/business name, phone, email, GST/PAN, Aadhaar, verification badge.
**[Primitive #5]: Service-Type Discriminator** — `serviceType` enum (SINGER / CAKE_DESIGNER / PANDIT / DECORATOR / STAFF / APPLIANCE / PHOTOGRAPHER / DJ / MEHENDI / MAKEUP) drives schema variant + UI form.
**[Primitive #6]: Service Area / Coverage** — City list + radius, or geo polygon.
**[Primitive #7]: Availability Calendar** — Date slots, lead-time, blackout dates.
**[Primitive #8]: Portfolio / Proof of Work** — Photos, audio reels, videos.
**[Primitive #9]: Pricing Model** — Flat / per-unit / per-hour / per-guest / tiered / quote-on-request.
**[Primitive #10]: Inclusions & Exclusions** — Sets customer expectation, prevents disputes.
**[Primitive #11]: Ratings, Reviews, Track Record** — Aggregate, computed (not vendor-entered).
**[Primitive #12]: Lifecycle Status** — DRAFT → PENDING_REVIEW → VERIFIED → PAUSED → SUSPENDED. **The admin-load-reducer** — admin only clicks Approve/Reject, never types data.

### Element 2 (cont.) — Trust & operations gaps

**[Primitive #13]: Cancellation & Refund Policy** — Vendor-set rules surfaced on listing page (Airbnb-style).
**[Primitive #14]: Compliance & Trust Documents** — FSSAI (cake/cook), IPRS (singer), police verification (staff entering homes), insurance (decor with electricals), gotra/tradition (pandit). India-specific.
**[Primitive #15]: Capacity & Concurrency Limits** — Max bookings/day, max kg/day, auto-pause at capacity.
**[Primitive #16]: Logistics — Owned Equipment & Crew** — Sound equipment, delivery vehicle, crew size, appliance stock count. Drives self-fulfill vs supply-service pull.
**[Primitive #17]: Lead Time, Slot Shape, Buffer** — Drives instant-book vs request-to-book.
**[Primitive #18]: Specialty Tags & Languages** — Pandit gotra+text-language+tradition; singer genre+language+religious-vs-secular; cook cuisine+diet; decor style.
**[Primitive #19]: Vendor-Owned Public Profile** — URL slug, hero, about-us, why-choose-us. Solves goal (b) — booking → catalog deep-link.
**[Primitive #20]: Multi-Person Team Roster** — Parent vendor + child team members each with own KYC + rating. Fixes staff-hire/decor modeling.

### MVP Scope Cut (agreed)

**MVP (Day-1 self-onboarding launch):** #1, #2, #3 (domain), #4 KYC, #5 type, #6 area, #7 calendar, #8 portfolio, #9 pricing, #12 lifecycle, #13 cancel-policy (legal exposure), #14 compliance — *FSSAI + police verification only*, #18 specialty tags, **#19 public profile** (kept in MVP because it ships goal-b — booking→catalog link — alongside goal-a).

**V2 / phase 2:** #11 ratings (needs booking history to compute), #14 broader compliance (IPRS, insurance, etc.), #15 capacity limits, #16 logistics/equipment, #17 lead-time/slot shape granularity, #20 team roster (staff-hire keeps current `staff_pool` until V2 unifies it).

## Phase 2 — Cross-Pollination (Analogical Thinking)

5 platforms screened, **all 5 patterns stolen** — A is the spine, others layer on top.

| Pattern | Source | What we steal | Implements |
|---|---|---|---|
| **A — Listing Wizard** | Airbnb | Per-`serviceType` guided wizard with draft-after-each-step. Admin only sees Approve/Reject, never types data. | #4, #7, #8, #12 — *the admin-load-reducer (goal a)* |
| **B — Service Catalog Items** | Urban Company | Vendor publishes *service items* (e.g. "3-tier chocolate truffle 1kg – ₹2,499") under their profile. Customer browses outcomes. | #5, #9, #19 |
| **C — Trust Badge Stack** | Practo | One-row icon stack: KYC ✓ / FSSAI ✓ / Police Verified ✓ / 4.8★ (47) / 5 yr on Safar. Trust without text. | #11, #14, #18 |
| **D — Shop with Items** | Etsy | `/vendors/{slug}` storefront + listed items. Booking row stores `vendorId + serviceItemId` → dashboard deep-links to *that exact item*. | #19 — *directly solves goal (b)* |
| **E — WhatsApp Onboarding** | Zomato BD | WhatsApp deep-link → mobile-first wizard, no app install. Critical for Indian vendors without desktops. | Drops onboarding friction; pairs with A |

**Sequencing decision:** A is shipped first as the spine (it's the load-reducer). B and D ship next together (they're inseparable — service items + storefront). C is mostly UI on top of existing data. E is a delivery mechanism on top of A.

## Phase 3 — Morphological Analysis

### Grid axes

**Rows (provider types) — MVP-first tiering**

| Tier | Types |
|---|---|
| **MVP launch** | CAKE_DESIGNER, SINGER, PANDIT, DECORATOR, STAFF_HIRE |
| **Migrate from current chef-service** | CHEF / COOK |
| **MVP-conditional** | APPLIANCE_RENTAL |
| **V2** | PHOTOGRAPHER, DJ, MEHENDI, MAKEUP |

**Columns (listing concerns)** — 9 columns derived from the 14 MVP primitives:
1. Core fields, 2. Type-specific fields, 3. KYC + compliance, 4. Pricing model, 5. Service items, 6. Calendar shape, 7. Coverage, 8. Search facets, 9. Migration source.

### Structural decisions (locked)

- **Q1 — Storage:** **Table-per-type with shared parent** (JOINED inheritance). One parent `service_listings` table holds the shared columns (vendor_id, business_name, slug, hero_image, status, kyc, ratings, coverage, lifecycle). One child table per `service_type` (`cake_attributes`, `singer_attributes`, `pandit_attributes`, ...) holds the varying part. Search runs on parent for shared facets, joins to child for type-specific facets.
- **Q2 — Service items:** **Optional 1:N from `service_listings`.** Only catalog-driven types (CAKE_DESIGNER, DECORATOR, PANDIT, APPLIANCE_RENTAL) populate `service_items`. SINGER, STAFF_HIRE, COOK have no items — vendor *is* the item; booking row stores `(service_listing_id, service_item_id?)` with item_id nullable.
- **Q3 — Service home:** **Rename `chef-service` → `services-service`** (port 8093 retained). CHEF/COOK become one of N `service_type` values within the new home. Smallest blast-radius rename: gateway routes + service name + package, no cross-service contract breakage.

### Worked example — CAKE_DESIGNER

| Col | Field set |
|---|---|
| **1. Core fields** | `business_name`, `vendor_slug`, `hero_image_url`, `tagline`, `about_md`, `founded_year`, `bakery_type` enum (HOME_BAKER/COMMERCIAL/CLOUD_KITCHEN), `signature_specialty` |
| **2. Type-specific fields** | `oven_capacity_kg_per_day`, `flavours_offered[]`, `design_styles[]`, `max_tier_count`, `eggless_capable`, `vegan_capable`, `delivery_mode` (SELF/PARTNER/PICKUP_ONLY) |
| **3. KYC + compliance** | Aadhaar+selfie, PAN, **FSSAI** (mandatory), GSTIN (conditional), Razorpay vendor onboard. Auto-verify FSSAI via online registry. |
| **4. Pricing model** | `base_price_per_kg_paise` × tier_surcharge × design_tier × eggless_surcharge + delivery_per_km + rush_surcharge. Stored as `pricing_formula` JSONB. |
| **5. Service items** | YES — required. Each item = a specific cake design with hero photo, weight/tier/flavour options, occasion tag. Booking targets `service_item_id` → goal (b) deep-link. |
| **6. Calendar shape** | `delivery_date` (day-grain, not slot). `default_lead_time_hours=48`, vendor-overridable. `blackout_dates[]`. `max_kg_per_day` deferred to V2. |
| **7. Coverage** | `cities[]` + per-city `(address_pin, delivery_radius_km)`. Beyond radius → `pickup_only` on booking. |
| **8. Search facets** | city, flavour, eggless/vegan, design_style, occasion_tag, price_band, rating, lead_time, bakery_type. |
| **9. Migration source** | `chef-service.partner_vendors WHERE service_type='CAKE_DESIGNER'` (8 rows). Cake bookings: `event_bookings.menu_description.type='DESIGNER_CAKE'` + `CAKE_DESIGNER` alias. Migration extracts → `service_listings` + `cake_attributes` + auto-creates one `service_items` row per existing baker. The DESIGNER_CAKE alias hack retires. |

### Worked example — SINGER

| Col | Field set |
|---|---|
| **1. Core fields** | `business_name` (artist stage name), slug, hero (performance shot), tagline, about, `years_performing`, `act_type` (SOLO/DUO/BAND/TROUPE) |
| **2. Type-specific fields** | `genres[]`, `languages[]`, `troupe_size_min/max`, `religious_capable`, `audio_reels[]`, `video_reels[]`, `equipment_owned` (FULL_PA/PARTIAL/NONE), `setup_time_minutes` |
| **3. KYC** | Aadhaar+selfie, PAN, GSTIN optional, Razorpay onboard. IPRS/PPL deferred to V2. |
| **4. Pricing** | `base_rate_per_hour_paise` × peak/off-peak band + travel_per_km + setup_fee + sound_addon. Quote-on-request flag. |
| **5. Service items** | **OPTIONAL — usually NO.** Singer = the item. Edge case: bands with fixed packages publish 2-3 items. |
| **6. Calendar** | **Slot-grain.** `start_time + duration_hours`, `available_slots[]` per date, lead-time ~7 days. |
| **7. Coverage** | cities + home_city radius + `outstation_capable` + `min_outstation_fee_paise`. |
| **8. Search facets** | city, **genre**, **language**, price_band, troupe_size, religious_secular, with-equipment, occasion, rating, response_time. |
| **9. Migration source** | `partner_vendors WHERE service_type='SINGER'` (none seeded — clean greenfield). Bookings: `LIVE_MUSIC` type. |

### Worked example — PANDIT

| Col | Field set |
|---|---|
| **1. Core fields** | `business_name` (often "Pandit Ramesh Sharma"), slug, hero, tagline, about, `years_practicing`, `acharya_or_purohit` |
| **2. Type-specific fields** | `tradition` (SMARTA/VAISHNAV/SHAIVITE/SINDHI/ARYA_SAMAJ/IYER/IYENGAR/MAITHIL/...), `pandit_gotra`, `text_languages[]` (Sanskrit + Hindi/Tamil/Bengali/Marathi/Gujarati), `shastra_specializations[]` (KARMA_KANDA/JYOTISH/VEDIC), `puja_types_offered[]`, `samagri_provided` (ALL/PARTIAL/NONE), `provides_dakshina_guidance` |
| **3. KYC** | Aadhaar+selfie, PAN, **lineage proof** (parampara letter or Vedic institution cert — admin reviews manually). |
| **4. Pricing** | **Flat per puja** (each item priced individually). Add-ons: samagri kit, travel, prasad. Tiers per puja: BASIC/STANDARD/PREMIUM. |
| **5. Service items** | **YES required.** Each item = one puja. Goal (b) deep-link surfaces tier/samagri/language/gotra customization. |
| **6. Calendar** | **Day-grain + auspicious muhurta window.** Multiple pujas/day capable. Festival HIGH_DEMAND badge instead of blackout. |
| **7. Coverage** | cities + radius + `outstation_capable` + `online_via_video_call` (post-COVID, online pujas are normal — unique to this type). |
| **8. Search facets** | city, **tradition**, **language**, **puja_type**, tier, price_band, rating, lead_time, online_capable, religion. |
| **9. Migration source** | `partner_vendors WHERE service_type='PANDIT'`. Bookings: `PANDIT_PUJA`. Migration auto-creates service_items rows from each pandit's advertised puja_types. |

### Worked example — DECORATOR

| Col | Field set |
|---|---|
| **1. Core fields** | `business_name`, slug, hero (best work shot), tagline, about, `years_in_business`, `team_size_min/max`, `setup_capacity_per_day` |
| **2. Type-specific fields** | `decor_styles[]` (PUNJABI/SOUTH_INDIAN/MARWARI/BENGALI/MODERN/RUSTIC/FUSION/THEME/MINIMALIST/ROYAL), `themes_executed[]` (free-form portfolio tags), `setup_capabilities[]` (FLORAL/LIGHTING/STAGE/MANDAP/SEATING/CENTERPIECES/BACKDROP/PROPS), `outdoor_capable`, `indoor_capable`, `largest_event_handled_pax`, `equipment_owned[]` (own crew vehicles, generator, lighting rigs), `crew_size_default` |
| **3. KYC + compliance** | Aadhaar+selfie, PAN, GSTIN (often applicable — turnover threshold), Razorpay onboard. V2: liability insurance (mandatory if using electricals/heavy props). |
| **4. Pricing** | **Quote-on-request primarily** + tiered packages secondarily. `pricing_pattern=QUOTE_ON_REQUEST` with optional `service_items` for fixed packages ("Standard Mandap Setup — ₹35,000", "Premium Stage Decor — ₹85,000"). |
| **5. Service items** | **OPTIONAL — package-driven.** Vendor publishes 0-N package tiers as items; customer can pick a package OR submit RFQ. Item fields: photos[], inclusions[], setup_hours, breakdown_hours, max_pax_supported. |
| **6. Calendar** | **Day-grain.** Decor is full-day; setup the day before, breakdown after. `default_lead_time_hours=72`. Concurrency = limited by crew_count (deferred to V2). |
| **7. Coverage** | cities + radius + outstation_capable + `transport_per_km`. Most decorators travel for big weddings. |
| **8. Search facets** | city, **decor_style** (must), **price_band** (must), occasion, indoor/outdoor, team_size, rating, lead_time. |
| **9. Migration source** | `partner_vendors WHERE service_type='DECORATOR'`. Bookings: `EVENT_DECOR`. Migration auto-creates 0-3 starter service_items per decorator from current pricing. |

### Worked example — STAFF_HIRE

| Col | Field set |
|---|---|
| **1. Core fields** | `business_name` (often agency name like "Cherry Crew Services"), slug, hero, tagline, about, `agency_type` (INDIVIDUAL_FREELANCER / SMALL_AGENCY / LARGE_AGENCY) |
| **2. Type-specific fields** | `roles_offered[]` (WAITER / BARTENDER / SERVER / KITCHEN_HELPER / HOSTESS / VALET / SECURITY / CLEANER / DJ_ASSISTANT / PHOTOGRAPHER_ASSISTANT), `min_count_per_booking` (some only do ≥4 staff), `max_count_per_booking`, `uniform_provided`, `experience_years_avg`, `languages_spoken[]`, `dress_codes_supported[]` (FORMAL_BLACK / SAREE / KURTA / EVENT_THEME) |
| **3. KYC + compliance** | Aadhaar+selfie, PAN, GSTIN if commercial agency, **POLICE_VERIFICATION** for every staff member entering customer homes (mandatory — safety gate for staff-hire), Razorpay onboard. |
| **4. Pricing** | **Per-time-block × per-head.** `base_rate_per_hour_per_person_paise` × shift duration × headcount + uniform/setup add-on + travel. |
| **5. Service items** | **OPTIONAL.** Same shape as singer — vendor *is* the item. Edge case: agencies with fixed packages ("Wedding Reception — 8 waiters + 2 bartenders × 6 hrs — ₹18K"). |
| **6. Calendar** | **Slot-grain.** `start_datetime + duration_hours + headcount`. Concurrency = staff_pool_available. |
| **7. Coverage** | cities + radius. Outstation rare for staff (locally sourced). |
| **8. Search facets** | city, **role** (must — waiter? bartender?), headcount, hours, occasion, rating, price_band, dress_code, languages. |
| **9. Migration source** | `partner_vendors WHERE service_type='STAFF'` + existing `staff_pool` table from session 2026-04-22. Migration: `staff_pool` rows become **child team-member rows** under a parent `staff_attributes` listing — implements Primitive #20 partially. Full team-roster (V2) extends this. |

### Validation outcome (5 rows total)

After CAKE / SINGER / PANDIT / DECOR / STAFF the schema holds. **No new top-level columns needed on the parent.** Two minor additions:

- `years_in_business` and `team_size_min/max` are common enough to lift to parent (already implicit in `founded_year`).
- `equipment_owned[]` keeps appearing across SINGER, DECOR, STAFF → could lift to parent as a generic `assets[]` text-array, but the *meaning* differs per type → keep in child tables (per JOINED inheritance discipline).

**Decision: schema as drafted is final.** Child tables for PHOTOGRAPHER / DJ / MEHENDI / MAKEUP / APPLIANCE / COOK can be added incrementally without touching parent.

### Insights crystallized from 3 rows

- **#4 — Cross-cutting tag axis.** `tradition`, `cuisine`, `genre`, `style`, `language` aren't type-specific — they're cultural facets that span types. → polymorphic `service_listing_tags(listing_id, namespace, value)`. Cross-type discovery becomes a feature ("planning a Punjabi wedding" → returns pandit + singer + cook + decor matching `STYLE=PUNJABI`).
- **#5 — Channel axis.** `delivery_channels[]` = (IN_PERSON / ONLINE / HYBRID) on the parent. Most types are IN_PERSON; pandit/cook/astrologer can opt-in.
- **#6 — Pricing patterns collapse to 4** (not N): PER_UNIT_TIERED (cake/decor), PER_TIME_BLOCK (singer/photographer), FLAT_PER_ITEM (pandit/staff/appliance), QUOTE_ON_REQUEST.
- **#7 — Calendar shapes collapse to 2** (not N): DAY_GRAIN vs SLOT_GRAIN. Single `service_listing_availability` table with discriminator.

---

## Schema Synthesis (Draft RFC)

### Service rename

`chef-service` → **`services-service`** (port 8093 retained, package `com.safar.chef` → `com.safar.services`). Gateway adds `/api/v1/services/**` route; old `/api/v1/chefs/**` kept as alias for ~2 sprints, then retired.

### Tables

**Parent — shared columns for ALL service listings:**

```sql
CREATE TABLE services.service_listings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_user_id UUID NOT NULL,                    -- the human/business who self-published (FK to auth.users)
  service_type VARCHAR(40) NOT NULL,               -- discriminator: CAKE_DESIGNER, SINGER, PANDIT, DECORATOR, STAFF_HIRE, COOK, ...

  -- Identity (#4, #19)
  business_name VARCHAR(200) NOT NULL,
  vendor_slug VARCHAR(100) UNIQUE NOT NULL,        -- powers /vendors/{slug} public profile (Pattern D)
  hero_image_url TEXT,
  tagline VARCHAR(280),
  about_md TEXT,
  founded_year INT,

  -- Lifecycle (#12) — the admin-load reducer
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',     -- DRAFT/PENDING_REVIEW/VERIFIED/PAUSED/SUSPENDED
  status_changed_at TIMESTAMPTZ,
  status_changed_by UUID,
  rejection_reason TEXT,

  -- Coverage (#6) + channel (Insight #5)
  cities TEXT[],
  home_city VARCHAR(100),
  home_pincode VARCHAR(10),
  home_lat NUMERIC(9,6),
  home_lng NUMERIC(9,6),
  delivery_radius_km INT,
  outstation_capable BOOLEAN DEFAULT FALSE,
  delivery_channels VARCHAR(20)[],                  -- IN_PERSON, ONLINE, HYBRID

  -- Pricing (#9) — Insight #6: 4 patterns collapse the N variants
  pricing_pattern VARCHAR(30) NOT NULL,             -- PER_UNIT_TIERED, PER_TIME_BLOCK, FLAT_PER_ITEM, QUOTE_ON_REQUEST
  pricing_formula JSONB,                            -- type-specific multipliers/surcharges

  -- Calendar discriminator (#7) — Insight #7
  calendar_mode VARCHAR(20),                        -- DAY_GRAIN, SLOT_GRAIN
  default_lead_time_hours INT,

  -- Cancellation policy (#13)
  cancellation_policy VARCHAR(30),                  -- FLEXIBLE, MODERATE, STRICT
  cancellation_terms_md TEXT,

  -- Aggregate ratings (#11) — denormalized for search-page perf
  avg_rating NUMERIC(3,2),
  rating_count INT DEFAULT 0,
  completed_bookings_count INT DEFAULT 0,

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_listings_type_status ON service_listings(service_type, status);
CREATE INDEX idx_listings_verified ON service_listings(status) WHERE status = 'VERIFIED';
CREATE INDEX idx_listings_vendor_user ON service_listings(vendor_user_id);
```

**Child tables — one per service_type (JOINED inheritance):**

```sql
CREATE TABLE services.cake_attributes (
  service_listing_id UUID PRIMARY KEY REFERENCES service_listings(id) ON DELETE CASCADE,
  bakery_type VARCHAR(30),                          -- HOME_BAKER, COMMERCIAL, CLOUD_KITCHEN
  oven_capacity_kg_per_day INT,
  flavours_offered VARCHAR(40)[],
  design_styles VARCHAR(40)[],
  max_tier_count INT,
  eggless_capable BOOLEAN,
  vegan_capable BOOLEAN,
  delivery_mode VARCHAR(20)
);

CREATE TABLE services.singer_attributes (
  service_listing_id UUID PRIMARY KEY REFERENCES service_listings(id) ON DELETE CASCADE,
  act_type VARCHAR(20),                             -- SOLO, DUO, BAND, TROUPE
  genres VARCHAR(40)[],
  languages VARCHAR(40)[],
  troupe_size_min INT,
  troupe_size_max INT,
  religious_capable BOOLEAN,
  audio_reels TEXT[],
  video_reels TEXT[],
  equipment_owned VARCHAR(20),                      -- FULL_PA, PARTIAL, NONE
  setup_time_minutes INT
);

CREATE TABLE services.pandit_attributes (
  service_listing_id UUID PRIMARY KEY REFERENCES service_listings(id) ON DELETE CASCADE,
  tradition VARCHAR(40),                            -- SMARTA, VAISHNAV, SHAIVITE, SINDHI, ARYA_SAMAJ, IYER, ...
  pandit_gotra VARCHAR(60),
  text_languages VARCHAR(40)[],
  shastra_specializations VARCHAR(40)[],
  puja_types_offered VARCHAR(40)[],
  samagri_provided VARCHAR(20),
  online_via_video_call BOOLEAN
);

-- decor_attributes, staff_attributes, appliance_attributes, cook_attributes, ...
-- One per type. Add tables — never columns to parent — when introducing new type.
```

**Service items — optional 1:N for catalog-driven types (Pattern B/D):**

```sql
CREATE TABLE services.service_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  service_listing_id UUID NOT NULL REFERENCES service_listings(id) ON DELETE CASCADE,
  title VARCHAR(200) NOT NULL,                       -- "3-tier Chocolate Truffle Birthday Cake"
  hero_photo_url TEXT,
  photos TEXT[],
  description_md TEXT,
  base_price_paise BIGINT NOT NULL,
  options_json JSONB,                                -- weight/tier/flavour/lang options — type-driven
  occasion_tags VARCHAR(40)[],
  lead_time_hours INT,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  display_order INT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_service_items_listing ON service_items(service_listing_id, status);
```

**Cross-cutting tags (Insight #4) — polymorphic facets:**

```sql
CREATE TABLE services.service_listing_tags (
  service_listing_id UUID NOT NULL REFERENCES service_listings(id) ON DELETE CASCADE,
  tag_namespace VARCHAR(20) NOT NULL,                -- LANGUAGE, TRADITION, STYLE, OCCASION, RELIGION
  tag_value VARCHAR(60) NOT NULL,
  PRIMARY KEY (service_listing_id, tag_namespace, tag_value)
);
CREATE INDEX idx_listing_tags_value ON service_listing_tags(tag_namespace, tag_value);
```

**Calendar (Insight #7) — discriminator-driven:**

```sql
CREATE TABLE services.service_listing_availability (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  service_listing_id UUID NOT NULL REFERENCES service_listings(id) ON DELETE CASCADE,
  date DATE NOT NULL,
  start_time TIME,                                   -- NULL for DAY_GRAIN
  end_time TIME,                                     -- NULL for DAY_GRAIN
  status VARCHAR(20) NOT NULL,                       -- AVAILABLE, BOOKED, BLACKOUT, HIGH_DEMAND
  booking_id UUID,
  notes TEXT
);
CREATE INDEX idx_availability_listing_date ON service_listing_availability(service_listing_id, date);
```

**KYC documents — uniform storage, type-specific requirements enforced in code:**

```sql
CREATE TABLE services.vendor_kyc_documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  service_listing_id UUID NOT NULL REFERENCES service_listings(id) ON DELETE CASCADE,
  document_type VARCHAR(40) NOT NULL,                -- AADHAAR, PAN, FSSAI, IPRS, POLICE_VERIFICATION, GST, LINEAGE_PROOF
  document_url TEXT NOT NULL,
  document_number VARCHAR(50),
  verification_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, VERIFIED, REJECTED
  verified_at TIMESTAMPTZ,
  verified_by UUID,
  expires_at DATE,
  uploaded_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_kyc_listing ON vendor_kyc_documents(service_listing_id);
```

### Booking impact

```sql
ALTER TABLE bookings.event_bookings
  ADD COLUMN service_listing_id UUID REFERENCES services.service_listings(id),
  ADD COLUMN service_item_id UUID REFERENCES services.service_items(id);     -- nullable; populated for catalog-driven types

CREATE INDEX idx_event_bookings_listing ON event_bookings(service_listing_id);
```

`menu_description JSON` stays as a snapshot of the customer's choices at booking time (immutable), but the canonical FK pair `(service_listing_id, service_item_id)` is what powers the "see what you booked" deep-link.

### Wizard architecture (Pattern A)

- Frontend route per type: `/vendor/onboard/cake`, `/vendor/onboard/singer`, `/vendor/onboard/pandit`.
- Shared `<OnboardingWizard>` React component, reads steps from `wizardSteps[serviceType]` config.
- Auto-saves to a `service_listings` row in `DRAFT` status after each step.
- Final step submits → `status: DRAFT → PENDING_REVIEW`.
- Admin queue: `/admin/service-listings?status=PENDING_REVIEW` — Approve / Reject + reason. Reject sends back to DRAFT for vendor to edit.
- WhatsApp deep-link (Pattern E): vendor BD sends `https://safar.com/vendor/onboard/{type}?invite={token}` → mobile-first wizard, phone OTP, prefilled.

### Migration plan (Flyway in `services` schema)

| Migration | What |
|---|---|
| **V23 (services-service)** | CREATE all new tables (parent + 5 MVP child tables + service_items + tags + availability + kyc_documents). |
| **V24 (services-service)** | Backfill from `chef-service.partner_vendors`. Each row → service_listings parent row (status=VERIFIED to grandfather them) + correct child attributes row + auto-create one service_items row per catalog-driven type using current data. |
| **V25 (booking-service)** | ALTER event_bookings ADD service_listing_id, service_item_id. Backfill from existing FK to partner_vendors. |
| **Deploy** | services-service starts; gateway routes live; wizard routes live; old `/api/v1/chefs/**` aliased to new endpoints for back-compat. |
| **V26 (services-service, +2 sprints)** | Drop `partner_vendors` once metrics confirm zero traffic on legacy paths. |

### Service rename mechanics

- Maven module `services/chef-service` → rename to `services/services-service`.
- Spring `@ComponentScan` base package → `com.safar.services`.
- application.yml: `spring.application.name: services-service`.
- Gateway `application.yml`: add `/api/v1/services/**` → services-service; keep `/api/v1/chefs/**` mapped for back-compat.
- Frontend `lib/api.ts`: helpers renamed (`createServiceListing`, `getMyServiceListings`, etc.); old `bookChef` etc. kept and proxied.
- DB schema name: keep `chefs` schema for now (rename is risky and decoupled from rest); new tables go in `services` schema. Future cleanup migration.

## Market Research Validation (2026-04-26)

Parallel competitive teardown of 14 platforms — full report at `_bmad-output/research/market-event-services-india-2026-04-26.md`.

### What the research VALIDATES (RFC unchanged)

| RFC decision | Validating signal |
|---|---|
| **Vendor-as-listing + optional `service_items`** | WedMeGood, ShaadiSaga, WeddingWire, The Knot all use vendor-profile-with-items-inside. UC/Bakingo's item-only-no-vendor model fits product-commerce, not personality-driven events. ✅ |
| **Table-per-type with type-specific fields** | Etsy's category-driven dynamic schema + Fiverr's category-mandatory pricing fields are our exact pattern. ✅ |
| **DRAFT → PENDING_REVIEW → VERIFIED lifecycle** | Universal in India — every platform has admin-Approve gate. Differentiator is *speed-to-approval* (target 24h SLA). ✅ |
| **Per-type KYC requirements (FSSAI mandatory for cake)** | FSSAI is **legally mandatory** for any e-commerce food sale (6-month imprisonment + ₹5L fine for violation). Not optional. ✅ |
| **4 pricing patterns** | Confirmed across 14 platforms — per-kg (Bakingo), per-hour (singer), per-event (pandit), quote (decor). ✅ |
| **`service_listing_tags` cross-cutting** | Etsy's per-category dynamic attributes are the exact analog. ✅ |

### What the research ADDS (RFC additions — fold in below)

#### Addition 1: **WhatsApp deep-link on booking detail = MVP, not V2**

Every successful India platform treats WhatsApp as a primary booking-comms channel. The booking detail page action bar must render:
- 💬 WhatsApp vendor (pre-filled with booking ID + service item)
- 📞 Call vendor
- ✉️ In-app chat
- 🔗 View ordered item (deep-link to `service_items.id`)

→ **New Primitive #21:** Multi-channel comms on every booking row. Already partially shipped in safar-web sprint-3 (OTP share buttons). Generalize.

#### Addition 2: **Practo's "Bluebook" → KYC-gates-service-type-claim**

Practo prevents over-claiming by mapping degree → permitted specializations. Steal:

```
verification_gates (config, not table):
  CAKE_DESIGNER  requires  [FSSAI]                 to publish
  COOK           requires  [FSSAI]                 to publish
  STAFF_HIRE     requires  [POLICE_VERIFICATION]   to publish
  PANDIT         requires  [LINEAGE_PROOF]         to publish
  SINGER         requires  []                       (no statutory gate)
  DECORATOR      requires  []
  APPLIANCE      requires  [GST]                    (commercial sale)
```

The wizard refuses to advance status from DRAFT → PENDING_REVIEW unless required `vendor_kyc_documents` are uploaded for the service_type. **Machine-enforceable; admin can't approve a cake baker without an FSSAI doc on file.** Big legal-exposure reduction.

→ Code change in `services-service`: `ServiceListingPublishValidator` reads `KYC_GATES_BY_TYPE` config + checks `vendor_kyc_documents` rows.

#### Addition 3: **Tiered "Verified" — JustDial's dual-gate**

JustDial's badge requires KYC + ≥3.8★. Don't give all credibility on day-one approval. Replace single VERIFIED status with a tier badge surfaced on the listing card:

| Badge | Requires |
|---|---|
| **Listed** | KYC verified, status=VERIFIED |
| **Safar Verified** | + 1+ completed booking + ≥4.0★ + ≥3 reviews |
| **Top Rated** | + 10+ bookings + ≥4.5★ + 90%+ on-time + 90%+ response |

Stored as a denormalized `trust_tier` enum on `service_listings`, computed nightly by a scheduled job. Surfaced as a single icon/text in trust-stack (Pattern C).

#### Addition 4: **Date-availability filter — easy India differentiator**

The Knot does this; **none of the India platforms researched do it cleanly**. Indian weddings cluster around <100 muhurat dates/year — date-availability is the #1 user constraint and the easiest competitive win.

→ The `service_listing_availability` table already supports this. Just expose a `?available_on=YYYY-MM-DD` filter on the search API. Frontend: date-picker on `/services/{category}` listing page.

#### Addition 5: **Occasion-led IA on /services landing (FNP pattern)**

FNP's primary navigation is "What are you celebrating?" not "What product?" Replace top-level service-type tiles with occasion-first picker:

```
Top: "I'm planning a..." → Birthday / Anniversary / Wedding / Housewarming / Pooja / Engagement / Mehendi / Sangeet
Drives: bundled service browse (cake + decor + singer + pandit suggested for that occasion)
```

→ Frontend-only change on /services landing. Drives bundling = goal (b) extension AND drives the *attachable layer* primitive #2 (services bundles into stay).

#### Addition 6: **Anti-patterns to AVOID** (encoded as RFC negative-space)

- ❌ **Don't auto-allocate vendors** for emotional-purchase categories (singer/pandit/decor). Urban Company / Housejoy hide the vendor — wrong for events. Customer must pick.
- ❌ **Don't go phone-only** comms (JustDial). Breaks SLA, kills booking deep-link, no rating capture.
- ❌ **Don't subscription-only** monetization (WedMeGood). India SMBs resist upfront fees. **Default = commission-on-booking; subscription as upgrade for power vendors.** This contradicts memory's listed STARTER ₹999/PRO ₹2,499/COMMERCIAL ₹3,999 — that pricing tier may need rethinking specifically for service-vendors (not stay hosts).
- ❌ **Don't ship vague "Verified" badges** without exposing what was verified — show "FSSAI ✓" not just "Verified ✓".
- ❌ **Don't force one schema** across categories — Etsy/Fiverr-style category-driven dynamic forms only.

### What the research raises as OPEN questions

1. Booking row → service_item deep-link is **largely absent** on researched competitors. **Goal (b) may be a genuine differentiator** for Safar, not table-stakes.
2. Whether WedMeGood actually verifies FSSAI/GST at onboarding — likely a gap to exploit (Safar enforces, they don't).
3. Hybrid catalog-driven (cake) vs RFQ-driven (decor/singer) UX in one platform — **no researched platform cleanly hybridizes**. Safar doing both per-vertical may be a novel design space.

## Monetization Decision Matrix (Open — needs product owner sign-off)

Existing memory shows commission tiers for **stay hosts**: STARTER ₹999 / PRO ₹2,499 / COMMERCIAL ₹3,999 monthly + commission %. Market research strongly suggests this model **does not transfer to service-vendors**.

### Three options

| Model | How it works | Pros | Cons |
|---|---|---|---|
| **A. Subscription-only** (current Safar host model) | Vendor pays ₹999/2,499/3,999 per month + 18%/12%/10% commission | Predictable platform revenue; aligns with existing host pricing | **WedMeGood evidence:** India SMB vendors resist upfront fees; high churn; high acquisition cost; small bakers/freelance singers won't sign up |
| **B. Commission-only** (Zomato / Urban Company pattern) | No monthly fee. Platform takes 12-18% per completed booking. Vendor zero-friction signup | Lowest vendor friction; aligns vendor incentives with platform; broad supply | Platform revenue lumpy in early days; encourages off-platform leakage if commission feels high |
| **C. Hybrid: Commission default + Subscription upgrade** (recommended) | New vendors join commission-only (12-18%, same rates as existing tiers). Power vendors opt into subscription for: lower commission % / unlimited featured slots / dedicated support / boosted placement | Lowest friction at signup; revenue grows with vendor success; subscription becomes a *reward*, not a tax | Two billing flows to maintain; subscription needs material perks to feel worth it |
| **D. Pay-per-vetted-lead** (Sulekha / Zola pattern) | Vendor pays only when a real customer conversation starts. No commission, no subscription | Pure pay-for-value; vendor controls spend | Hard to price-discover; doesn't capture booking-execution rev; weak incentive for SLA |

### Recommendation: **Option C (Hybrid)**

**Default vendor experience:**
- Commission-on-booking only. Same per-tier % as today (STARTER 18% / PRO 12% / COMMERCIAL 10%) — *but tier is now earned by booking volume*, not paid for.
- Auto-tier promotion: cross 10 bookings → PRO; cross 50 bookings → COMMERCIAL.

**Subscription as upgrade (optional):**
- ₹999/month — Featured Vendor: priority placement on `/services/{category}` listing, 2 dedicated leads/month, badge on storefront. No commission change.
- ₹2,499/month — Pro Vendor: lower commission (10% flat regardless of tier), unlimited featured placement, response-time SLA support, monthly performance call.

**Why this works for India:**
- Removes signup-day cash barrier (the biggest WedMeGood failure).
- Subscription becomes aspirational ("when I'm doing well"), not punitive.
- Doesn't conflict with existing stay-host pricing because service-vendors are operationally distinct.

### Decision needed before Sprint 1 launch

This is a product/business decision, not an engineering one. Schema is **already neutral** to all 4 options — `service_listings` doesn't encode pricing model; that lives in a separate `vendor_billing_plan` config (to be added in Sprint 4 when needed). Engineering can ship A/B/C/D from the same data model.

## Session Closeout

### Deliverables produced

| Artifact | Location | Status |
|---|---|---|
| Brainstorm session record | `_bmad-output/brainstorming/brainstorming-session-2026-04-26-event-services-gap.md` | this file |
| Market research teardown (14 platforms) | `_bmad-output/research/market-event-services-india-2026-04-26.md` | written |
| Formal PRD | `safar-platform/docs/prd-services-leg.md` | written |
| Sprint plan (4 sprints) | `safar-platform/docs/sprint-plan-services-leg.md` | written |
| V23 migration SQL | `services/chef-service/src/main/resources/db/migration/V23__create_services_leg_schema.sql` | written |
| JPA entity skeleton | `services/chef-service/src/main/java/com/safar/chef/entity/ServiceListing.java` + `CakeAttributes.java` + `ServiceItem.java` + `enums/ServiceListingStatus.java` | written |
| Repositories | `services/chef-service/src/main/java/com/safar/chef/repository/ServiceListingRepository.java` + `ServiceItemRepository.java` | written |

### Brainstorm goals — final status

- ✅ **Goal A** (vendor self-onboarding, drop admin load) — schema + lifecycle + KYC gates designed; sprint 1-2 implementation planned
- ✅ **Goal B** (booking → "see what you booked" deep-link) — `service_items` table + booking FK + storefront UX in sprint 3; identified as **genuine differentiator** (no researched competitor solves this)
- ✅ **Goal C** (rename chef-service → services-service, provider taxonomy) — sprint 1; mechanics captured
- ✅ **Goal D** (one connected design) — single PRD, single migration train, single deploy window

### Top 6 design moves (from market research) folded in

1. WhatsApp deep-link on every booking row (MVP, not V2)
2. Practo "Bluebook" → KYC-gates-service-type at publish time
3. JustDial dual-gate → tiered Verified badge (LISTED / SAFAR_VERIFIED / TOP_RATED)
4. The Knot date-availability filter (competitive win — no India platform has this)
5. FNP occasion-led IA on /services landing
6. Anti-patterns encoded as negative-space (no auto-allocation, no phone-only, no subscription-only, no vague-Verified, no one-form-fits-all)

### Open decisions before Sprint 1 kicks off

1. **Monetization model** — Option A/B/C/D from §"Monetization Decision Matrix" (recommend C — Hybrid)
2. **Schema home** — keep new tables in new `services` schema vs fold into `chefs` schema (recommend new schema as written)
3. **WhatsApp Business API** — manual BD outreach vs templated MSG91 integration (recommend templated for scale)
4. **Listing edits post-VERIFIED** — silent vs re-review (recommend silent for non-material, re-review for material)
5. **Slug uniqueness** — globally unique vs scoped per service_type (recommend globally unique as written)


