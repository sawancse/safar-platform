# Sprint Plan: Services Leg — First-Class Vendor Listings

**Reference:** `docs/prd-services-leg.md`
**Date:** 2026-04-26
**Estimated total:** 4 sprints (~4 weeks of focused work)

---

## Sequencing principle

A is the spine (admin-load reducer), B+D ship together (storefront + items + deep-link), C is UI on top of existing data, E is delivery mechanism on top of A.

```
Sprint 1: Spine (rename + schema + admin queue)
Sprint 2: Wizard (vendor self-onboarding for cake first, then replicate)
Sprint 3: Items + Storefront + Booking deep-link (goal B end-to-end)
Sprint 4: Polish (date filter, occasion IA, trust tier, WhatsApp, retire partner_vendors)
```

---

## Sprint 1 — Spine (~1 week)

**Goal:** Rename service, create schema, make admin queue functional. No vendor-facing UI yet.

### Backend

| # | Task | Estimate | Notes |
|---|---|---|---|
| 1.1 | Rename `chef-service` Maven module → `services-service` (port stays 8093) | 0.5 d | Package `com.safar.chef` → `com.safar.services`; update parent pom + module list |
| 1.2 | application.yml: `spring.application.name: services-service` | 0.1 d | |
| 1.3 | api-gateway: add `/api/v1/services/**` route → services-service | 0.2 d | Keep `/api/v1/chefs/**` aliased for ~2 sprints |
| 1.4 | **V23 migration** — create `services` schema + parent `service_listings` + 5 MVP child tables (cake/singer/pandit/decor/staff) + `service_items` + `service_listing_tags` + `service_listing_availability` + `vendor_kyc_documents` | 1 d | DDL drafted in brainstorm; copy + tweak |
| 1.5 | JPA entities — `ServiceListing` parent + 5 child entities (JOINED inheritance) + `ServiceItem` + `ServiceListingTag` + `ServiceListingAvailability` + `VendorKycDocument` | 1.5 d | Lombok + MapStruct |
| 1.6 | Repositories | 0.3 d | Standard Spring Data JPA |
| 1.7 | `ServiceListingService` — CRUD + lifecycle transitions (DRAFT/PENDING_REVIEW/VERIFIED/PAUSED/SUSPENDED) + ownership checks | 1 d | |
| 1.8 | `ServiceListingPublishValidator` — KYC gate (Bluebook pattern) | 0.5 d | Reads `KYC_GATES_BY_TYPE` config |
| 1.9 | Admin endpoints — `GET /admin/service-listings?status=PENDING_REVIEW`, `POST /admin/service-listings/{id}/approve`, `POST /admin/service-listings/{id}/reject` | 0.5 d | |
| 1.10 | Kafka outbox: `service.listing.published` / `service.listing.suspended` | 0.5 d | Match existing outbox pattern |
| 1.11 | Unit + integration tests | 1 d | |

### Frontend (admin)

| # | Task | Estimate | Notes |
|---|---|---|---|
| 1.12 | Admin page `/admin/service-listings` — queue table with filter, Approve / Reject modal | 1 d | Match existing admin patterns (Ant Design) |
| 1.13 | Document viewer for KYC docs (Aadhaar, FSSAI, lineage proof, etc.) | 0.5 d | S3 presigned-URL fetch |

**Sprint 1 done when:** ops can approve a listing that exists only in DB (no vendor UI yet); listing flows DRAFT → PENDING_REVIEW → VERIFIED with KYC gate enforced.

---

## Sprint 2 — Wizard (~1 week)

**Goal:** Vendor can self-onboard cake → admin approves. Replicate for singer + pandit + decor + staff.

### Frontend (safar-web)

| # | Task | Estimate | Notes |
|---|---|---|---|
| 2.1 | Shared `<OnboardingWizard>` component — step config + auto-save + draft state | 1 d | Reads `wizardSteps[serviceType]` |
| 2.2 | `wizardSteps.cake` — 8 steps: identity, bakery type, KYC docs (incl. FSSAI), portfolio photos, flavours/styles, pricing formula, coverage cities, lead time | 1 d | |
| 2.3 | Route `/vendor/onboard/cake` | 0.3 d | |
| 2.4 | KYC doc upload component (image picker + S3 upload via media-service) | 0.5 d | |
| 2.5 | `wizardSteps.singer` (~6 steps; no items) | 0.5 d | |
| 2.6 | `wizardSteps.pandit` (~7 steps; lineage proof gate) | 0.5 d | |
| 2.7 | `wizardSteps.decor` (~6 steps; quote-on-request flag) | 0.5 d | |
| 2.8 | `wizardSteps.staff_hire` (~7 steps; police verification gate) | 0.5 d | |
| 2.9 | `/vendor/dashboard` skeleton — list "My listings" + edit/pause buttons | 1 d | |

### Backend

| # | Task | Estimate | Notes |
|---|---|---|---|
| 2.10 | Listing draft auto-save endpoint (PATCH partial) | 0.3 d | |
| 2.11 | Listing submit endpoint (DRAFT → PENDING_REVIEW with KYC gate) | 0.3 d | |
| 2.12 | Vendor "my listings" endpoint (filtered by `vendor_user_id`) | 0.2 d | |
| 2.13 | Tests | 0.5 d | |

**Sprint 2 done when:** a baker who knows the URL can fill the wizard end-to-end on mobile, submit, and see the listing in PENDING_REVIEW; admin can approve and listing goes VERIFIED.

---

## Sprint 3 — Items + Storefront + Booking Deep-Link (~1 week)

**Goal:** Goal (B) end-to-end. Vendor publishes service items; customer books an item; booking detail deep-links back.

### Backend

| # | Task | Estimate | Notes |
|---|---|---|---|
| 3.1 | `ServiceItem` CRUD endpoints — vendor publishes/edits/deletes items under their listing | 0.5 d | |
| 3.2 | **V43 (booking-service)** — ALTER `event_bookings` ADD `service_listing_id`, `service_item_id` (nullable). Backfill from existing FK to partner_vendors | 0.5 d | |
| 3.3 | booking-service create-booking flow — accept `service_listing_id` + optional `service_item_id`; validate against services-service | 0.5 d | |
| 3.4 | Public listing lookup endpoint — `GET /api/v1/services/listings/{slug}` (renders public profile) | 0.3 d | Cached |
| 3.5 | Public items list — `GET /api/v1/services/listings/{slug}/items` | 0.2 d | |

### Frontend

| # | Task | Estimate | Notes |
|---|---|---|---|
| 3.6 | `/vendor/dashboard` — "My items" tab; create/edit item modal with type-specific options form | 1 d | Cake form ≠ pandit form |
| 3.7 | `/services/{category}/{vendor-slug}` — public storefront page (Pattern D — Etsy shop). Hero, about, items grid, trust stack | 1.5 d | Items as anchors `#item-{id}` |
| 3.8 | Customer order flow updates — pass `service_listing_id` + `service_item_id` to booking creation | 0.5 d | Cake / decor / pandit / appliance order pages |
| 3.9 | `/cooks/my-bookings/[id]` — add "View this {cake/puja/singer}" deep-link button → storefront `#item-{id}` | 0.5 d | Reuse existing `<ServiceBookingDetails>` |
| 3.10 | Trust-stack component on storefront (KYC ✓ / FSSAI ✓ / 5 yr / 4.8★ / 47 reviews) | 0.5 d | Reusable |
| 3.11 | Tests | 0.5 d | |

**Sprint 3 done when:** customer books a cake from a storefront → opens booking → sees the cake photo + clicks "View this cake" → lands on the exact item on the storefront.

---

## Sprint 4 — Polish + Cutover (~1 week)

**Goal:** Date filter, occasion IA, trust tier, WhatsApp, retire `partner_vendors`.

### Frontend (additions)

| # | Task | Estimate | Notes |
|---|---|---|---|
| 4.1 | `/services` landing — occasion-led IA picker ("I'm planning a Birthday/Wedding/Pooja...") → drives bundled service browse | 1 d | FNP pattern |
| 4.2 | `/services/{category}` listing page — add `?available_on=YYYY-MM-DD` filter chip + date picker | 0.5 d | Competitive differentiator (no India platform has this) |
| 4.3 | `/cooks/my-bookings/[id]` action bar — Call/WhatsApp/SMS buttons on every active booking (generalize OTP share) | 0.5 d | |
| 4.4 | Public storefront — surface trust tier badge (LISTED / SAFAR_VERIFIED / TOP_RATED) | 0.3 d | |
| 4.5 | WhatsApp deep-link onboarding flow — `?invite={token}` query param prefills phone + skip OTP | 0.5 d | Pattern E |

### Backend

| # | Task | Estimate | Notes |
|---|---|---|---|
| 4.6 | Search endpoint — `GET /api/v1/services/search?category=&city=&available_on=&style=&...` | 1 d | Joins parent + tags + availability |
| 4.7 | Trust-tier scheduler — nightly recompute on `service_listings.trust_tier` based on bookings + ratings | 0.5 d | `@Scheduled` |
| 4.8 | Listener: `booking.completed` → update `completed_bookings_count`, `avg_rating`, `rating_count`; `review.created` → same | 0.5 d | |
| 4.9 | **V24 (services-service)** — backfill from `chef-service.partner_vendors` (grandfather as VERIFIED) | 0.5 d | |
| 4.10 | Smoke-test all migrated bookings + listings; flag any that fail | 0.5 d | |
| 4.11 | **V25 (services-service, only after smoke-test passes)** — drop `partner_vendors`. Retire `DESIGNER_CAKE → CAKE_DESIGNER` alias hack | 0.3 d | Deferrable to next sprint if anxious |

### Cleanup

| # | Task | Estimate | Notes |
|---|---|---|---|
| 4.12 | Remove `/api/v1/chefs/**` gateway alias (after metrics confirm zero traffic) | 0.2 d | Can also defer |

**Sprint 4 done when:** customers can search by date, occasion-led browse works, trust tiers compute, partner_vendors retired, vendor self-onboarding end-to-end on prod.

---

## Cross-cutting / not-in-sprints

- **WhatsApp Business API integration** — coordinate with notification-service for templated outreach. Stretch — could move to V2.
- **Vendor mobile app** — V2. Wizard is mobile-web for MVP.
- **Photographer / DJ / Mehendi / Makeup child tables** — add 1 child table per sprint as demand surfaces.
- **Safar Cooks heading rename + "Track Chef" rename** — already in deferred-from-sprint-3 list. Bundle into Sprint 4.

---

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| Vendor wizard abandonment mid-flow | Auto-save to DRAFT on every step; resume URL via WhatsApp; progress bar |
| Admin queue depth grows past 24-hr SLA | KYC gate machine-enforced before queue; quick-Approve UX |
| FSSAI verification is manual + slow | V1: admin reviews doc image. V2: integrate FSSAI online verification API |
| Schema drift between `chefs.partner_vendors` and `services.service_listings` during overlap window | Both writable for ~2 sprints; cutover scheduler reconciles nightly |
| `service_item_id` not being populated by older booking flows | Make NULL allowed; backfill batch; instrument the field on booking creation |
| Singer / staff vendors have no items, breaking storefront UI | Storefront falls back to "Book this vendor" CTA when items=[] |

---

## Definition of Done (whole initiative)

- [ ] Vendor can self-onboard cake/singer/pandit/decor/staff via mobile-web wizard
- [ ] Admin clicks Approve on PENDING_REVIEW listings; KYC gate prevents incomplete submits
- [ ] Booking detail page shows the exact `service_item` booked with deep-link
- [ ] `/services` landing has occasion-led IA
- [ ] `/services/{category}` has date-availability filter
- [ ] Trust tier computes nightly + surfaces on storefront
- [ ] WhatsApp deep-link onboarding works
- [ ] `partner_vendors` table retired
- [ ] `chef-service` renamed to `services-service` (gateway alias kept)
- [ ] CHEF/COOK migrated to first-class `service_type` rows alongside the 4 new types
