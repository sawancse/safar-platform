# PRD: Safar Services Leg — First-Class Vendor Listings

**Version:** 1.0
**Date:** 2026-04-26
**Status:** Design phase — schema RFC ready, sprint planning next
**Owner:** Platform / Marketplace
**Source:** `_bmad-output/brainstorming/brainstorming-session-2026-04-26-event-services-gap.md` + `_bmad-output/research/market-event-services-india-2026-04-26.md`

---

## 1. Executive Summary

Safar is a 3-leg trip-and-occasion platform: **stay + flight + services**. Today the stay leg has first-class listings (`listing-service`) and the flight leg has searchable inventory (`flight-service`). The services leg lives in `chef-service` as an admin-curated `partner_vendors` directory — singers, cake bakers, pandits, decorators, staff-hire, appliance vendors are all admin-seeded rows, not self-publishable listings. A baker cannot self-onboard the way a host self-onboards a property. Booking detail pages collapse to empty placeholders for non-cook services because there's no canonical service-item to deep-link to.

This PRD ships **first-class service listings**: vendor self-onboards via per-type wizard → admin clicks Approve → vendor is live. Booking rows store `(service_listing_id, service_item_id)` so customer dashboards render "the cake/puja/singer you booked" with a deep-link back to the listing. `chef-service` is renamed `services-service`; CHEF/COOK become one service-type among 11.

**Why now:** the bespoke services marketplace shipped over the last 4 sprints (cake, decor, pandit, live-music, staff-hire, appliances) is producing customer demand admin can't keep up with — every new vendor is a manual seed-data entry. Self-onboarding is the unit-economics fix.

---

## 2. Goals & Non-Goals

### 2.1 Goals (MVP)

| ID | Goal | Success criterion |
|---|---|---|
| **A** | Vendor self-onboards with admin only clicking Approve | Admin onboarding load ↓ ≥80%; vendor publish-to-live ≤24 hr SLA |
| **B** | Booking detail deep-links to the exact service-item booked | Customer can re-open the cake/puja/singer they booked from `/cooks/my-bookings/{id}` with photo + options |
| **C** | Rename `chef-service` → `services-service`; provider taxonomy with 11 types | All `service_type`s share a parent table + child attributes; CHEF/COOK is one of N |
| **D** | (A) + (B) + (C) ship as one connected design | Single PR train, single migration set, single deploy window |

### 2.2 Non-Goals (explicit deferrals)

- Vendor self-rating / cross-vendor "Certified Reviews" (V2 — needs booking history first)
- Capacity / concurrency limits per listing (V2 — defer until vendors complain)
- Logistics / equipment owned (V2 — needed for supply-service integration, not for launch)
- Multi-person team roster (V2 — current `staff_pool` keeps working until then)
- Photographer / DJ / Mehendi / Makeup as service-types (V2 — add after MVP launch validates)
- IPRS/PPL music license verification for singers (V2 — defer; freelance artists rarely have it)
- Insurance docs upload + verification (V2)
- Online video pujas (V2 — Pandit `online_via_video_call` flag schema-supported but no booking flow)
- Vendor mobile app (V2 — wizard is mobile-web in MVP)

### 2.3 Not-doing (architectural decisions)

- **No auto-allocation** of vendors (Urban Company / Housejoy pattern). Customers pick their cake baker/singer/pandit — events are personality-driven.
- **No phone-only routing** (JustDial pattern). All comms thread through Safar booking-id-bearing chat or WhatsApp template.
- **No subscription-only** monetization (WedMeGood pattern). India SMBs resist upfront fees — see §10 monetization.

---

## 3. User Stories

### 3.1 Vendor (Self-Onboarding)

- **As a cake baker**, I open a WhatsApp deep-link from a Safar BD rep → onboarding wizard pre-filled with my phone → I upload FSSAI license + Aadhaar + portfolio photos + set price-per-kg → I submit → admin approves within 24 hrs → my listing is live at `/services/cake/sweet-symphony-bakery`.
- **As a pandit**, I cannot publish without uploading lineage proof (KYC gate enforced at submit time).
- **As a singer**, I publish without IPRS — no statutory gate for me.
- **As a vendor**, I can pause my listing when I'm on holiday and resume later (PAUSED status, reversible).

### 3.2 Customer

- **As a customer browsing `/services`**, I pick "I'm planning a Birthday" → see cake + decor + singer + balloons all suggested for that occasion.
- **As a customer who booked a designer cake**, I open my booking → I see the *exact* cake design photo + flavour + tier I picked, with a "View this cake" deep-link to the baker's listing item.
- **As a customer**, I can WhatsApp the vendor pre-filled with my booking ID directly from the booking detail page.
- **As a customer**, I can filter `/services/cake?city=Hyderabad&available_on=2026-05-15` and see only bakers free that date.

### 3.3 Admin

- **As ops**, my "vendor onboarding" inbox shows only PENDING_REVIEW listings — I review docs, click Approve / Reject + reason. I never type vendor data.
- **As ops**, I can suspend a misbehaving vendor (status=SUSPENDED) — vendor's listing hides immediately; existing bookings honored.
- **As ops**, I see a "Safar Verified" tier (computed) on listings that have completed ≥1 booking with ≥4.0★ rating + ≥3 reviews.

---

## 4. Architecture

### 4.1 Service & Schema

- **Rename:** `chef-service` → `services-service` (port 8093 retained, package `com.safar.chef` → `com.safar.services`).
- **Database schema:** new `services` schema (existing `chefs` schema retained for chef/event/staff-pool tables; `services` holds the new listing model).
- **Java package:** `com.safar.services.listing` for listing, `com.safar.services.item` for service-items, `com.safar.services.kyc` for documents.

### 4.2 Why rename, not new service

- All cross-service contracts are by URL prefix (`/api/v1/chefs/**`) and Kafka topic name; renaming the service rebrands the codebase but keeps existing `chefs` schema accessible for back-compat.
- Old `/api/v1/chefs/**` aliased to new endpoints for ~2 sprints, then retired.
- New `/api/v1/services/**` routes added on api-gateway from day one.

### 4.3 Inter-service flows

- **services-service** ↔ **booking-service** — booking-service reads `services.service_listings` + `services.service_items` to validate booking creation; stores `service_listing_id` + `service_item_id` on `event_bookings`.
- **services-service** → Kafka:
  - `service.listing.published` (status → VERIFIED)
  - `service.listing.suspended`
  - `service.item.created/updated/deleted`
- **services-service** ← Kafka:
  - `booking.completed` → recompute aggregate ratings + trust tier
  - `review.created` → recompute aggregate ratings

### 4.4 New tables (in `services` schema)

| Table | Purpose | Cardinality |
|---|---|---|
| `service_listings` | Parent — shared columns for ALL service types | ~10K rows in 1 yr |
| `cake_attributes` | Child 1:1 — cake-specific fields | One per cake listing |
| `singer_attributes` | Child 1:1 — singer-specific | One per singer listing |
| `pandit_attributes` | Child 1:1 — pandit-specific | One per pandit listing |
| `decor_attributes` | Child 1:1 — decor-specific | One per decor listing |
| `staff_attributes` | Child 1:1 — staff-hire specific | One per staff listing |
| `service_items` | Optional 1:N — vendor's published items (catalog-driven types) | ~50K rows in 1 yr |
| `service_listing_tags` | Polymorphic — cross-cutting facets (LANGUAGE/TRADITION/STYLE/OCCASION/RELIGION) | ~5/listing |
| `service_listing_availability` | Calendar with discriminator (DAY_GRAIN / SLOT_GRAIN) | ~365/listing/yr |
| `vendor_kyc_documents` | Uniform doc storage; type-specific requirements enforced in code | ~3/listing |

Full DDL: see `_bmad-output/brainstorming/brainstorming-session-2026-04-26-event-services-gap.md` § "Schema Synthesis".

---

## 5. Lifecycle & Status Flow

```
DRAFT  ←──── (vendor edits)
  │
  ▼ (vendor submits, KYC gate enforced)
PENDING_REVIEW
  │       │
  │       ▼ (admin rejects + reason)
  │      DRAFT
  ▼ (admin approves)
VERIFIED ←─── (vendor un-pauses)
  │             ▲
  │             │
  ▼ (vendor pauses)
PAUSED
  │
  ▼ (admin suspends — misbehavior)
SUSPENDED ──→ DRAFT (admin restores)
```

- `DRAFT` — wizard auto-saves here; not visible to customers.
- `PENDING_REVIEW` — admin queue; vendor cannot edit.
- `VERIFIED` — visible, bookable, indexed in search.
- `PAUSED` — vendor self-pause (holiday, on-leave); not visible/bookable; vendor can resume.
- `SUSPENDED` — admin-only; not visible; admin can restore to DRAFT.

---

## 6. KYC Gating (Practo "Bluebook" pattern)

`ServiceListingPublishValidator` reads from `KYC_GATES_BY_TYPE` config. Submit DRAFT → PENDING_REVIEW only succeeds if all required `vendor_kyc_documents` rows exist and are `verification_status=VERIFIED` or `PENDING`.

| service_type | Required documents | Optional |
|---|---|---|
| CAKE_DESIGNER | Aadhaar, PAN, **FSSAI** | GSTIN |
| COOK | Aadhaar, PAN, **FSSAI** | GSTIN |
| SINGER | Aadhaar, PAN | GSTIN, IPRS |
| PANDIT | Aadhaar, PAN, **LINEAGE_PROOF** | — |
| DECORATOR | Aadhaar, PAN | GSTIN, INSURANCE |
| STAFF_HIRE | Aadhaar, PAN, **POLICE_VERIFICATION** | GSTIN |
| APPLIANCE_RENTAL | Aadhaar, PAN, **GST** | — |

**FSSAI is legally mandatory** for any e-commerce food sale (6-month imprisonment + ₹5L fine for violation). Non-negotiable gate.

---

## 7. Trust Tier (JustDial dual-gate pattern)

Computed nightly by scheduled job; denormalized to `service_listings.trust_tier`.

| Tier | Requires |
|---|---|
| **LISTED** | KYC verified, status=VERIFIED |
| **SAFAR_VERIFIED** | + 1+ completed booking + ≥4.0★ + ≥3 reviews |
| **TOP_RATED** | + 10+ bookings + ≥4.5★ + 90%+ on-time + 90%+ response rate |

Surfaced as a single icon/text in the trust-stack on listing card and detail page. Trust-stack also exposes underlying credentials separately ("FSSAI ✓", "5 yr on Safar", "47 reviews") — never just "Verified ✓".

---

## 8. Frontend (safar-web)

### 8.1 New routes

- `/vendor/onboard/{type}` — per-type wizard (cake / singer / pandit / decor / staff). Mobile-first.
- `/vendor/dashboard` — vendor's listings, bookings, payouts.
- `/services/{category}/{vendor-slug}` — public vendor profile (Pattern D — Etsy shop with items).
- `/services` — landing page with **occasion-led IA** (FNP pattern) — primary picker = "I'm planning..." not "What service?"

### 8.2 Existing routes that change

- `/cooks/my-bookings/[id]` — already has `<ServiceBookingDetails>` from sprint-3; add deep-link button "View this {cake/puja/singer}" → `/services/{category}/{slug}#item-{id}`.
- `/cooks/my-bookings/[id]` action bar — add WhatsApp/Call/SMS buttons on every active row (already partial via OTP share — generalize).
- `/services/{category}` listing page — add `?available_on=YYYY-MM-DD` filter (date availability — competitive differentiator).

### 8.3 Wizard architecture

Shared `<OnboardingWizard>` component reads `wizardSteps[serviceType]` config:
- Each step = `{ title, fields[], validation, draftKey }`.
- Auto-saves to `service_listings` row in `DRAFT` status after each step.
- Final step submits → POST `/api/v1/services/listings/{id}/submit` → status → PENDING_REVIEW.
- WhatsApp deep-link entry: `https://safar.com/vendor/onboard/{type}?invite={token}` → mobile-first, phone OTP, prefilled.

### 8.4 Admin queue

- New admin page: `/admin/service-listings?status=PENDING_REVIEW` (target ≤24 hr SLA from queue depth).
- Rows show: vendor name, service_type, KYC docs status, submitted_at age.
- One-click Approve / Reject (with reason) — never typing data.

---

## 9. Migration Plan

| Step | Migration | What |
|---|---|---|
| 1 | **V23 (chef-service)** | CREATE all new tables (parent + 5 MVP child tables + service_items + tags + availability + kyc_documents) in `services` schema. |
| 2 | **V24 (chef-service)** | Backfill from `chef-service.partner_vendors`. Each row → `service_listings` parent (status=VERIFIED to grandfather) + correct child attributes row + auto-create one `service_items` row per catalog-driven type using current data. |
| 3 | **V43 (booking-service)** | ALTER `event_bookings` ADD `service_listing_id`, `service_item_id` (nullable). Backfill from existing FK to partner_vendors. |
| 4 | **Code deploy** | services-service (renamed) ships; gateway adds `/api/v1/services/**`; old `/api/v1/chefs/**` aliased; wizard routes live; admin page live. |
| 5 | **V25 (chef-service, +2 sprints)** | Drop `partner_vendors` once metrics confirm zero traffic on legacy paths. The `DESIGNER_CAKE → CAKE_DESIGNER` alias hack retires here. |

---

## 10. Monetization (Decision Pending)

Existing memory shows commission tiers: STARTER ₹999 / PRO ₹2,499 / COMMERCIAL ₹3,999 monthly + commission %.

Market research indicates **subscription-only fails for India SMBs** (WedMeGood evidence). For service-vendors specifically (not stay hosts), recommend:

- **Default:** commission-on-booking only (no monthly fee). Same % per existing tier (18% / 12% / 10%).
- **Upgrade:** opt-in subscription for power vendors who want unlimited leads / boosted placement / dedicated support.
- **Fallback:** Sulekha-style pay-per-vetted-lead if commission proves too friction-heavy.

**Decision needed before launch.** Defer to product owner.

---

## 11. Success Metrics (3-month post-launch)

| Metric | Target |
|---|---|
| Self-onboarded vendors / month | ≥50 (vs current admin-seeded ~10) |
| Admin manual data-entry per vendor | <5 min (vs current ~30 min) |
| Vendor publish-to-live SLA | ≤24 hr p95 |
| Bookings with `service_item_id` set | ≥70% of catalog-driven bookings |
| Customer "see this cake/puja again" deep-link clicks | ≥30% of post-booking sessions |
| Vendor active rate (bookings/month) | ≥30% in month 2, ≥50% in month 3 |
| KYC gate rejections (incomplete docs) | <20% of submissions (high → wizard UX problem) |

---

## 12. Open Questions

1. **Monetization** — commission-only vs hybrid for service-vendors? (See §10.)
2. **Schema name** — keep new tables in new `services` schema, or fold into existing `chefs` schema after rename? Recommend new schema for clean migration boundary.
3. **WhatsApp deep-link templating** — manual vendor BD outreach vs. integrated MSG91 WhatsApp Business API templates?
4. **Listing edits post-VERIFIED** — silent edit vs. re-review queue? Recommend silent for non-material fields (description, photos), re-review for material (price, KYC).
5. **Slug uniqueness across types** — `sweet-symphony-bakery` taken by cake → can singer also use `sweet-symphony`? Recommend globally unique.

---

## 13. References

- Brainstorm: `_bmad-output/brainstorming/brainstorming-session-2026-04-26-event-services-gap.md`
- Market research: `_bmad-output/research/market-event-services-india-2026-04-26.md`
- Sprint plan: `docs/sprint-plan-services-leg.md`
- Related: `docs/prd-supply-chain.md` (SCM), `docs/prd-vas-professional-management.md`
