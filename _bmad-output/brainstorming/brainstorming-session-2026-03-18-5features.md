# BMAD Brainstorming + Implementation: 5 Airbnb/Booking.com-Inspired Features
**Date:** 2026-03-18
**Techniques:** Morphological Analysis, First Principles, SCAMPER
**Research:** Airbnb & Booking.com business model deep-dive

---

## Research Summary

### Airbnb (2025)
- 8M active listings, peer-to-peer marketplace
- **New 15.5% host-only fee** (replacing 3% host + 14% guest split)
- Escrow model: collects 100% at booking, pays host 24h after check-in
- AirCover: $3M damage protection
- Superhost: earned (4.8+ rating, <1% cancel, 10+ stays, 90% response)
- Smart Pricing: AI daily adjustments
- Categories: vibe-based discovery ("OMG!", "Treehouses", "Amazing pools")
- Double-blind reviews: both revealed simultaneously
- Flexible payments: "Pay Part Now, Part Later", "Reserve Now, Pay Later"

### Booking.com (2025)
- 28M listings, commission-based OTA
- 15% avg commission (10-25% range), guest pays ₹0
- Genius loyalty: 3 tiers (10-20% discounts), 45% more bookings for partners
- Pay at Property option (cash at check-in)
- Non-refundable rates (10-15% cheaper)
- Preferred Partner (pay more commission → rank higher)
- Rate plans: non-refundable, flexible, breakfast-included, mobile-only
- Extranet: professional dashboard for property management
- Visibility Booster: temporary ranking boost for specific dates

### Safar's Competitive Advantage
- Subscription pricing (₹999-₹3,999/mo + 0-18%) vs Airbnb 15.5% vs Booking.com 15-25%
- Instant UPI payouts vs Airbnb 24h vs Booking.com NET 30
- India-first: regional languages, PG/co-living, Aashray refugee housing

---

## 5 Features Selected & Implemented

### Feature 1: Non-Refundable Rate Plan (from Booking.com)

**Why:** Reduces India's massive cancellation problem. Guests get 10% discount for committing.

**Implementation:**
- `CancellationPolicy` enum: added `NON_REFUNDABLE`
- `Listing.nonRefundableDiscountPercent` — host configures (default 10%)
- `Booking.nonRefundable` flag + `nonRefundableDiscountPaise`
- `BookingService`: `basePrice -= basePrice * discountPct / 100` when non-refundable
- `CreateBookingRequest`: added `Boolean nonRefundable` field

**Files Modified:**
- `listing-service/entity/enums/CancellationPolicy.java` — added NON_REFUNDABLE
- `booking-service/entity/Booking.java` — added nonRefundable, nonRefundableDiscountPaise
- `booking-service/dto/CreateBookingRequest.java` — added nonRefundable, paymentMode
- `booking-service/service/BookingService.java` — discount calculation logic

**Migration:** V46 (listing), V20 (booking)

---

### Feature 2: Pay at Property (from Booking.com)

**Why:** 70% of Indian travelers prefer cash/UPI at check-in. Critical for trust.

**Implementation:**
- New `PaymentMode` enum: `PREPAID`, `PAY_AT_PROPERTY`, `PARTIAL_PREPAID`
- `Listing.payAtPropertyEnabled` — host opt-in
- `Listing.partialPrepaidPercent` — e.g. 30% now, 70% at property
- `Booking.paymentMode`, `prepaidAmountPaise`, `dueAtPropertyPaise`
- `PAY_AT_PROPERTY`: prepaid=0, due=total
- `PARTIAL_PREPAID`: configurable split (default 30/70)

**Files Created:**
- `listing-service/entity/enums/PaymentMode.java`

**Files Modified:**
- `listing-service/entity/Listing.java` — paymentMode, payAtPropertyEnabled, partialPrepaidPercent
- `booking-service/entity/Booking.java` — paymentMode, prepaidAmountPaise, dueAtPropertyPaise
- `booking-service/service/BookingService.java` — payment split logic
- `booking-service/service/ListingServiceClient.java` — getNonRefundableDiscountPercent, getPartialPrepaidPercent

**Migration:** V46 (listing), V20 (booking)

---

### Feature 3: India Discovery Categories (from Airbnb Categories)

**Why:** Browse by vibe, not just location. Emotional discovery drives bookings.

**28 Categories:**
- Nature: HILL_STATIONS, BEACH_HOUSES, LAKE_VIEWS, MOUNTAIN_RETREATS, MONSOON_RETREATS
- Heritage: HERITAGE_HAVELIS, PALACE_STAYS, TEMPLE_TOWNS, COLONIAL_BUNGALOWS
- Unique: TREEHOUSES, HOUSEBOATS, FARM_STAYS, TINY_HOMES, GLAMPING, CAVE_STAYS
- Lifestyle: WORK_FROM_HILLS, DIGITAL_NOMAD, YOGA_WELLNESS, AYURVEDA
- Family: WEEKEND_GETAWAYS, FAMILY_FRIENDLY, PET_FRIENDLY, POOL_PARTIES
- Special: DIWALI_SPECIALS, WEDDING_VENUES, CORPORATE_OFFSITES, BUDGET_GEMS

**Implementation:**
- `DiscoveryCategory` enum — 28 India-specific categories
- `Listing.discoveryCategories` — comma-separated (max 3 per listing)
- `DiscoveryCategoryService` — browse, auto-suggest based on city/title/amenities
- `DiscoveryCategoryController` at `/api/v1/discover/`
  - `GET /categories` — all categories with listing counts (public)
  - `GET /categories/{category}` — browse listings by category (public)
  - `PUT /listings/{id}/categories` — host sets categories (max 3)
  - `GET /listings/{id}/suggest-categories` — AI suggestions

**Auto-suggestion logic:**
- Manali/Shimla/Ooty → HILL_STATIONS, WORK_FROM_HILLS
- Goa/Pondicherry/Gokarna → BEACH_HOUSES
- Jaipur/Udaipur/Jodhpur → HERITAGE_HAVELIS
- "farm"/"organic" in description → FARM_STAYS
- petFriendly=true → PET_FRIENDLY
- basePricePaise < ₹2,000/night → BUDGET_GEMS

**Files Created:**
- `listing-service/entity/enums/DiscoveryCategory.java`
- `listing-service/service/DiscoveryCategoryService.java`
- `listing-service/controller/DiscoveryCategoryController.java`

**API Gateway:** `/api/v1/discover/**` → listing-service

**Migration:** V46

---

### Feature 4: Safar Star Host (from Airbnb Superhost)

**Why:** Motivates hosts to maintain quality. Guests trust Star Host badge.

**Criteria (all must be met):**
| Metric | Threshold |
|--------|-----------|
| Average host rating | ≥ 4.8 / 5.0 |
| Cancellation rate | < 2% |
| Completed stays (12 months) | ≥ 10 |
| Response rate | ≥ 90% |

**Implementation:**
- `UserProfile.starHost` (boolean), `starHostSince`, `avgHostRating`, `cancellationRatePercent`, `totalCompletedStays`
- `StarHostService`:
  - `qualifies(host)` — checks all 4 criteria
  - `evaluate(hostId)` — awards or revokes badge
  - `updateHostMetrics(hostId, ...)` — called via Kafka from booking/review services
  - `quarterlyEvaluation()` — @Scheduled on 1st of Jan/Apr/Jul/Oct at 3 AM
  - `getCriteria(hostId)` — returns progress toward Star Host for dashboard display

**Badge earned:** Star Host status displayed on listing cards, host profile, search results
**Badge lost:** Quarterly re-evaluation. If criteria not met → badge removed.

**Files Created:**
- `user-service/service/StarHostService.java`
- `user-service/repository/ProfileRepository.java` — added findByRole, findByStarHostTrue

**Files Modified:**
- `user-service/entity/UserProfile.java` — 5 new fields

**Migration:** V10 (user-service)

---

### Feature 5: Double-Blind Reviews (from Airbnb)

**Why:** Prevents retaliation bias. Both parties review honestly without seeing the other's review first.

**How it works:**
```
Checkout → 14-day review window opens
  ├─ Guest submits review (hidden)
  ├─ Host submits review of guest (hidden)
  └─ BOTH revealed simultaneously when:
       a) Both have submitted, OR
       b) 14-day deadline expires (whichever comes first)
```

**Implementation:**
- `Review.hostRating` (Short 1-5) — host rates the guest
- `Review.hostComment` (TEXT) — host's written review
- `Review.hostReviewedAt` — when host submitted
- `Review.guestReviewVisible` (boolean) — false until reveal
- `Review.hostReviewVisible` (boolean) — false until reveal
- `Review.bothRevealedAt` — timestamp when both revealed
- `Review.reviewDeadline` — checkout + 14 days

**ReviewService methods:**
- `submitHostReview(bookingId, hostId, rating, comment)` — host submits independently
- `tryRevealDoubleBlind(review)` — checks if both done → reveals
- `revealExpiredReviews()` — daily job reveals past-deadline reviews

**New endpoint:** `POST /api/v1/reviews/host-review/{bookingId}?rating=5&comment=...`

**Files Modified:**
- `review-service/entity/Review.java` — 7 new fields
- `review-service/repository/ReviewRepository.java` — added findByBookingId
- `review-service/service/ReviewService.java` — double-blind logic
- `review-service/service/BookingServiceClient.java` — added getCheckOutForBooking
- `review-service/controller/ReviewController.java` — new host-review endpoint

**Migration:** V4 (review-service)

---

## Migration Summary

| Service | Migration | Content |
|---------|-----------|---------|
| listing-service | V46 | non_refundable_discount, payment_mode, pay_at_property, discovery_categories |
| booking-service | V20 | non_refundable, payment_mode, prepaid/due amounts |
| user-service | V10 | star_host badge fields |
| review-service | V4 | double-blind review fields |

## New Endpoints Summary

| Endpoint | Feature | Auth |
|----------|---------|------|
| `GET /api/v1/discover/categories` | Discovery categories | Public |
| `GET /api/v1/discover/categories/{cat}` | Browse by category | Public |
| `PUT /api/v1/discover/listings/{id}/categories` | Set categories | Host |
| `POST /api/v1/reviews/host-review/{bookingId}` | Host reviews guest | Host |
| Booking API: `nonRefundable=true` | Non-refundable rate | Guest |
| Booking API: `paymentMode=PAY_AT_PROPERTY` | Pay at property | Guest |

## Safar's Competitive Position After These 5 Features

| Feature | Airbnb | Booking.com | Safar |
|---------|--------|-------------|-------|
| Non-refundable rate | ❌ No | ✅ Yes | ✅ **Yes (just added)** |
| Pay at property | ❌ No (escrow only) | ✅ Yes | ✅ **Yes (just added)** |
| Vibe categories | ✅ Yes (global) | ❌ No | ✅ **Yes (India-specific)** |
| Earned host badge | ✅ Superhost | ❌ Preferred (paid) | ✅ **Star Host (earned)** |
| Double-blind reviews | ✅ Yes | ❌ No (guest-only) | ✅ **Yes (just added)** |
| Subscription pricing | ❌ 15.5% commission | ❌ 15-25% commission | ✅ **₹999-₹3,999/mo** |
| Instant UPI payout | ❌ 24h delay | ❌ NET 30 | ✅ **Instant** |
| PG/Co-living | ❌ No | ❌ Limited | ✅ **Full support** |
| Regional languages | ❌ No Indian | ❌ Limited | ✅ **5 languages** |
| Refugee housing | ❌ No | ❌ No | ✅ **Aashray** |
