# PRD: Safar Aashray Donation System

**Product**: Safar Platform — Aashray Module
**Author**: Safar Product Team
**Status**: Shipped (v1.0)
**Date**: 2026-04-01
**Sprint**: S21

---

## 1. Overview

### 1.1 Problem Statement

India hosts 300,000+ refugees (Tibetan, Afghan, Rohingya, Sri Lankan Tamil, Myanmar Chin) and 25M+ climate-displaced persons annually. While Safar Aashray already connects displaced families with host properties and NGO case workers, there is no mechanism for individual citizens to financially contribute to the housing fund. The `/aashray/donate` link on the main Aashray page returned a 404 — a dead end for motivated donors.

### 1.2 Solution

A full-stack donation system that enables individuals and organizations to fund refugee housing through one-time or recurring monthly contributions, with Razorpay payment processing, 80G tax receipt generation, and real-time social proof to drive conversions.

### 1.3 Success Metrics

| Metric | Target (90 days) | Measurement |
|--------|-------------------|-------------|
| Total donations | ₹5,00,000 | `donation_stats.total_raised_paise` |
| Unique donors | 200+ | `donation_stats.total_donors` |
| Monthly SIP donors | 50+ | `donation_stats.monthly_donors` |
| Conversion rate (page view → donation) | 3-5% | Analytics |
| Repeat donor rate | 20%+ | Donors with 2+ captured donations |
| Average donation | ₹2,000-3,000 | Sum / count |
| WhatsApp share rate (post-donation) | 30%+ | Click tracking |

### 1.4 Non-Goals (v1)

- Directed funding to specific families or cities (pooled model only)
- Corporate CSR portal with CIN/CSR registration fields
- Razorpay Route auto-split to hosts (Phase 2)
- Donor leaderboard / gamification
- Festival-themed auto-campaigns (Diwali, Eid, Republic Day)
- Donor impact dashboard within user profile

---

## 2. Market Research

Analyzed 6 leading platforms to identify best-practice UX patterns for the Indian donation market:

| Platform | Key Pattern Adopted |
|----------|-------------------|
| **UNHCR** | Impact-labeled amount tiles ("₹500 = 1 night of shelter") |
| **Ketto.org** | "SIP" framing for monthly giving (mutual fund familiarity); recent donor ticker |
| **GiveIndia** | 80G tax calculator inline; "receipt within 24 hours" guarantee |
| **Milaap** | Before/After story cards; WhatsApp-first sharing |
| **GlobalGiving** | Donor tier badges (Friend / Builder / Champion / Patron) |
| **Habitat for Humanity** | Fund allocation visual bar; progress toward housing goal |

### Patterns Implemented (17 total)

1. Impact-labeled amount tiles with "Most Popular" anchor (₹2,500)
2. Inline 80G tax savings calculator ("Save ₹X, effective cost only ₹Y")
3. "Aashray SIP" — monthly recurring framed as SIP
4. Live progress bar from backend API
5. Recent donor ticker (rotating social proof)
6. Donor tier badges (Friend / Builder / Champion / Patron)
7. Trust badge strip (80G Certified, NGO Verified, Razorpay Secure)
8. Fund allocation visual bar (70/15/10/5 split)
9. Real-time per-category breakdown based on selected amount
10. Employer matching prompt (TCS, Infosys, Google, etc.)
11. Before/After beneficiary story cards
12. WhatsApp share as primary post-donation CTA
13. PAN field for 80G certificate
14. Dedication option ("In honor of...")
15. UPI-first Razorpay checkout configuration
16. 8 FAQ items (tax, SIP, payment methods, employer matching)
17. Dev mode mock payment for testing

### Patterns Deferred (Phase 2)

- Donor leaderboard (opt-in, city-wise)
- Impact dashboard in user profile
- Festival-timed auto-campaigns
- "Gift a shelter" shareable certificate
- One-tap repeat donation for returning donors
- Corporate/bulk donation flow with CSR fields

---

## 3. User Stories

### 3.1 Donor (Anonymous)

| ID | Story | Priority |
|----|-------|----------|
| D-1 | As an anonymous visitor, I can donate any amount ≥ ₹100 without creating an account | P0 |
| D-2 | As a donor, I can choose between one-time and monthly recurring donations | P0 |
| D-3 | As a donor, I can see exactly what my money provides (impact tiers) | P0 |
| D-4 | As a donor, I can pay via UPI, cards, net banking, or wallets | P0 |
| D-5 | As a donor, I can provide my PAN to receive an 80G tax certificate | P0 |
| D-6 | As a donor, I can see how much I'll save in taxes before paying | P1 |
| D-7 | As a donor, I can dedicate my donation in honor of someone | P1 |
| D-8 | As a donor, I can share my donation on WhatsApp after paying | P1 |

### 3.2 Donor (Authenticated)

| ID | Story | Priority |
|----|-------|----------|
| D-9 | As a logged-in donor, my donation is linked to my Safar account | P0 |
| D-10 | As a logged-in donor, I can view my donation history | P1 |
| D-11 | As a logged-in donor, I earn a donor tier badge on my profile | P2 |

### 3.3 Platform Visitor

| ID | Story | Priority |
|----|-------|----------|
| V-1 | As a visitor, I can see how much has been raised toward the goal | P0 |
| V-2 | As a visitor, I can see recent donations (social proof) | P1 |
| V-3 | As a visitor, I can read beneficiary stories with before/after | P1 |
| V-4 | As a visitor, I can see where the money goes (transparent breakdown) | P0 |

### 3.4 Admin

| ID | Story | Priority |
|----|-------|----------|
| A-1 | As an admin, I can view all donations with status filtering | P0 |
| A-2 | As an admin, I can update campaign goal and tagline | P1 |
| A-3 | As an admin, I can update families housed count | P1 |

---

## 4. Technical Architecture

### 4.1 System Diagram

```
┌──────────────────┐     ┌──────────────┐     ┌─────────────────────┐
│   safar-web      │     │  API Gateway  │     │  payment-service    │
│  /aashray/donate │────▶│  :8080        │────▶│  :8086              │
│  (Next.js 14)    │     │              │     │                     │
│                  │     │  /api/v1/    │     │  DonationController │
│  ┌────────────┐  │     │  donations/**│     │  DonationService    │
│  │ Razorpay   │  │     └──────────────┘     │  DonationRepository │
│  │ Checkout   │  │                          │                     │
│  │ (UPI-first)│  │     ┌──────────────┐     │  ┌───────────────┐  │
│  └─────┬──────┘  │     │  Razorpay    │◄────│──│ RazorpayGateway│  │
│        │         │     │  API         │     │  └───────────────┘  │
│        ▼         │     └──────────────┘     │                     │
│  verify donation │                          │  ┌───────────────┐  │
│  → show receipt  │     ┌──────────────┐     │──│ Kafka Producer │  │
│  → WhatsApp share│     │ notification │◄────│  │ donation.      │  │
└──────────────────┘     │ -service     │     │  │ captured       │  │
                         │ (80G email)  │     │  └───────────────┘  │
                         └──────────────┘     └─────────────────────┘
                                                       │
                                              ┌────────▼────────┐
                                              │   PostgreSQL     │
                                              │   payments schema│
                                              │   ┌───────────┐  │
                                              │   │ donations  │  │
                                              │   │ donation_  │  │
                                              │   │ stats      │  │
                                              │   └───────────┘  │
                                              └─────────────────┘
```

### 4.2 Data Model

#### `donations` table

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | Auto-generated |
| `donation_ref` | VARCHAR(30) UNIQUE | e.g. `DON-2026-1001` |
| `donor_id` | UUID (nullable) | Linked Safar user (null = anonymous) |
| `donor_name` | VARCHAR(255) | For 80G receipt |
| `donor_email` | VARCHAR(255) | For receipt delivery |
| `donor_phone` | VARCHAR(20) | Optional |
| `donor_pan` | VARCHAR(10) | For 80G certificate |
| `amount_paise` | BIGINT | Amount in paise (1 INR = 100 paise) |
| `currency` | VARCHAR(3) | Default `INR` |
| `frequency` | VARCHAR(20) | `ONE_TIME` or `MONTHLY` |
| `razorpay_order_id` | VARCHAR(100) UNIQUE | For one-time payments |
| `razorpay_payment_id` | VARCHAR(100) UNIQUE | After capture |
| `razorpay_subscription_id` | VARCHAR(100) | For monthly SIP |
| `payment_method` | VARCHAR(30) | upi, card, netbanking, wallet |
| `status` | VARCHAR(20) | `CREATED`, `CAPTURED`, `FAILED`, `REFUNDED` |
| `dedicated_to` | VARCHAR(255) | Dedication name |
| `dedication_message` | TEXT | Dedication message |
| `campaign_code` | VARCHAR(50) | e.g. `diwali-2026` |
| `receipt_number` | VARCHAR(30) UNIQUE | e.g. `80G-2026-1001` |
| `receipt_sent` | BOOLEAN | Default `false` |
| `captured_at` | TIMESTAMPTZ | Payment capture timestamp |
| `created_at` | TIMESTAMPTZ | Row creation |
| `updated_at` | TIMESTAMPTZ | Last update |

**Indexes**: `donor_id`, `status`, `campaign_code`, `captured_at DESC`

#### `donation_stats` table (singleton)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | Single row |
| `total_raised_paise` | BIGINT | Running total |
| `goal_paise` | BIGINT | Campaign goal (default ₹5,00,000) |
| `total_donors` | INTEGER | Unique donor count |
| `families_housed` | INTEGER | Estimated families helped |
| `monthly_donors` | INTEGER | Active SIP donors |
| `active_campaign` | VARCHAR(100) | Current campaign name |
| `campaign_tagline` | VARCHAR(255) | Campaign description |
| `updated_at` | TIMESTAMPTZ | Last update |

**Flyway migration**: `V9__donations.sql`

### 4.3 API Endpoints

| Method | Path | Auth | Request | Response | Description |
|--------|------|------|---------|----------|-------------|
| `POST` | `/api/v1/donations` | Public | `CreateDonationRequest` | `DonationOrderResponse` | Create Razorpay order or subscription |
| `POST` | `/api/v1/donations/verify` | Public | `VerifyDonationRequest` | `DonationResponse` | Verify payment signature, capture |
| `GET` | `/api/v1/donations/stats` | Public | — | `DonationStatsResponse` | Progress bar, social proof ticker |
| `GET` | `/api/v1/donations/{ref}` | Auth | — | `DonationResponse` | Get donation by reference |
| `GET` | `/api/v1/donations/my` | Auth | Pageable | `Page<DonationResponse>` | Donor's own history |
| `GET` | `/api/v1/donations/admin` | Auth | status, Pageable | `Page<DonationResponse>` | Admin: all donations |

#### Request/Response DTOs

**CreateDonationRequest**
```json
{
  "amountPaise": 250000,
  "frequency": "ONE_TIME",
  "donorName": "Priya Sharma",
  "donorEmail": "priya@gmail.com",
  "donorPan": "ABCDE1234F",
  "dedicatedTo": "My grandmother",
  "dedicationMessage": null,
  "campaignCode": null
}
```

**DonationOrderResponse**
```json
{
  "donationRef": "DON-2026-1001",
  "razorpayOrderId": "order_NxHg...",
  "razorpaySubscriptionId": null,
  "amountPaise": 250000,
  "currency": "INR",
  "razorpayKeyId": "rzp_live_...",
  "frequency": "ONE_TIME"
}
```

**DonationStatsResponse**
```json
{
  "totalRaisedPaise": 12500000,
  "goalPaise": 50000000,
  "totalDonors": 87,
  "familiesHoused": 12,
  "monthlyDonors": 23,
  "progressPercent": 25,
  "activeCampaign": null,
  "campaignTagline": null,
  "recentDonors": [
    { "name": "P***a", "amountPaise": 250000, "city": null, "minutesAgo": 3 }
  ]
}
```

### 4.4 Payment Flows

#### One-Time Donation

```
1. User selects amount + fills optional fields
2. Frontend → POST /api/v1/donations (amountPaise, frequency=ONE_TIME)
3. Backend → Razorpay Orders API → creates order
4. Backend → saves Donation entity (status=CREATED)
5. Backend → returns DonationOrderResponse with razorpayOrderId
6. Frontend → opens Razorpay Checkout (UPI-first config)
7. User completes payment in Razorpay modal
8. Razorpay → calls handler with payment_id + signature
9. Frontend → POST /api/v1/donations/verify (orderId, paymentId, signature)
10. Backend → verifies HMAC signature via RazorpayGateway
11. Backend → updates Donation (status=CAPTURED, receiptNumber generated)
12. Backend → updates DonationStats (totalRaised, donors, familiesHoused)
13. Backend → publishes Kafka event: donation.captured
14. notification-service → sends 80G receipt email
15. Frontend → shows thank-you screen with WhatsApp share
```

#### Monthly SIP Donation

```
1-2. Same as one-time, but frequency=MONTHLY
3. Backend → Razorpay Plans API → creates plan
4. Backend → Razorpay Subscriptions API → creates subscription
5. Backend → saves Donation with razorpaySubscriptionId
6. Frontend → opens Razorpay Checkout with subscription_id
7-15. Same verification + capture flow
    Razorpay handles recurring charges automatically
```

### 4.5 Security

| Concern | Implementation |
|---------|---------------|
| Anonymous donations | `POST /donations`, `POST /verify`, `GET /stats` bypass JWT |
| Authenticated donors | Token passed optionally; `donorId` linked if present |
| Payment integrity | Razorpay HMAC-SHA256 signature verification |
| Webhook security | Razorpay webhook signature validation |
| PAN storage | Stored as plain VARCHAR(10) — no PII encryption needed (tax ID, not sensitive) |
| Donor name masking | Social proof ticker shows `P***a` not full name |
| Rate limiting | Inherited from API Gateway (standard rate limits) |

### 4.6 Infrastructure

| Component | Detail |
|-----------|--------|
| Backend | payment-service on ECS (Fargate), port 8086 |
| Gateway | api-gateway routes `/api/v1/donations/**` to payment-service |
| Database | PostgreSQL 16, `payments` schema |
| Events | Kafka topic `donation.captured` |
| Frontend | Next.js 14 on AWS Amplify (auto-deploy on git push) |
| Payments | Razorpay (Orders API + Subscriptions API) |

---

## 5. Frontend Specification

### 5.1 Page: `/aashray/donate`

**URL**: `https://www.ysafar.com/aashray/donate`
**Bundle size**: 7.65 kB (static, client-rendered)

### 5.2 Page Sections

| # | Section | Description |
|---|---------|-------------|
| 1 | **Hero** | Amber-orange-red gradient, "Give the gift of shelter", trust badge strip |
| 2 | **Progress Bar** | Live stats from `/stats` API: raised vs goal, donors, families, SIPs |
| 3 | **Recent Donor Ticker** | Rotating: "P***a donated ₹2,000 — 3m ago" (4s rotation) |
| 4 | **Amount Selector** | 6 presets (₹500-25K) with impact labels, "Most Popular" on ₹2,500 |
| 5 | **One-time / SIP Toggle** | "Aashray SIP (Monthly)" with SIP explainer |
| 6 | **Custom Amount** | Expandable input, min ₹100 |
| 7 | **80G Tax Calculator** | Green card: "Save ₹X in taxes, effective cost ₹Y" |
| 8 | **Donor Tier Badge** | Amber card: "You'll earn the 'Shelter Builder' badge" |
| 9 | **Donor Info** | Name, email (2-col), PAN, dedication (expandable) |
| 10 | **Donate Button** | Full-width gradient, shows amount + frequency |
| 11 | **Payment Methods** | UPI, Cards, Net Banking, Wallets text |
| 12 | **Impact Sidebar** | Checklist of impact tiers (checkmarks for reached tiers) |
| 13 | **Fund Allocation** | Visual bar (70/15/10/5) with per-amount breakdown |
| 14 | **Employer Matching** | Blue prompt card |
| 15 | **Stories** | 3 beneficiary cards with before/after comparison |
| 16 | **FAQ** | 8 expandable questions |
| 17 | **Bottom CTA** | "Other ways to help" — host / NGO links |

### 5.3 Thank You Screen

| Element | Detail |
|---------|--------|
| Animation | Bouncing prayer emoji + ping dots |
| Personalization | "Thank You, {firstName}!" if name provided |
| Donor badge | Tier badge displayed |
| Receipt | 80G receipt number shown |
| Email confirmation | "Receipt emailed to {email}" |
| WhatsApp share | Green button, pre-filled message with donate link |
| Navigation | "Back to Aashray" + "Go Home" buttons |

### 5.4 Responsive Design

- **Desktop**: 5-column grid (3 form + 2 sidebar)
- **Tablet**: Stacks to single column below `lg` breakpoint
- **Mobile**: Full-width, amount grid becomes 2-col on small screens

---

## 6. Commission & Pricing

| Aspect | Value |
|--------|-------|
| Minimum donation | ₹100 (10,000 paise) |
| Platform commission | 0% (Aashray tier) |
| Razorpay processing fee | ~2% (borne by platform, not donor) |
| 80G tax deduction | 50% of donation amount |
| Default campaign goal | ₹5,00,000 |
| Family housing estimate | ₹10,000 = 1 family-month |

### Fund Allocation

| Category | Percentage | Use |
|----------|-----------|-----|
| Rent & Deposits | 70% | Direct payment to Aashray hosts |
| Essential Supplies | 15% | Bedding, kitchenware, furnishings |
| Case Worker Support | 10% | NGO coordination and matching |
| Platform Operations | 5% | Technology, verification, processing |

---

## 7. Donor Tier System

| Tier | Min Amount | Badge | Emoji |
|------|-----------|-------|-------|
| Shelter Friend | ₹500 | Shelter Friend | :handshake: |
| Shelter Builder | ₹2,000 | Shelter Builder | :hammer: |
| Shelter Champion | ₹5,000 | Shelter Champion | :trophy: |
| Shelter Patron | ₹15,000 | Shelter Patron | :crown: |

Badges are shown on the donation confirmation screen. Profile display is planned for Phase 2.

---

## 8. Impact Tiers

| Amount | Impact Description |
|--------|--------------------|
| ₹500 | 1 night of safe shelter |
| ₹2,500 | 1 week in a safe home |
| ₹10,000 | 1 full month of housing |
| ₹25,000 | 3 months — a fresh start |

---

## 9. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Low conversion rate | Medium | High | 80G calculator + social proof ticker + "Most Popular" anchor |
| Razorpay subscription failures | Low | Medium | Webhook handlers for failed charges; manual retry |
| Fraudulent donations (money laundering) | Low | High | PAN collection; Razorpay KYC; admin review for large amounts |
| 80G receipt compliance | Medium | High | Auto-generate receipt numbers; email within 24hrs |
| Donor fatigue | Medium | Medium | Monthly SIP framing; impact updates; WhatsApp virality |
| Stats manipulation | Low | Medium | Materialized stats updated only on CAPTURED donations |

---

## 10. Phase 2 Roadmap

| Feature | Description | Priority |
|---------|-------------|----------|
| Donor Impact Dashboard | Profile tab showing total donated, families helped, timeline | P1 |
| Festival Campaigns | Auto-themed banners for Diwali, Eid, Republic Day | P1 |
| Corporate CSR Portal | CIN registration, bulk receipts, partner listing | P1 |
| Directed Funding | Donate to specific city or family | P2 |
| Gift a Shelter Certificate | Shareable digital certificate for dedications | P2 |
| Donor Leaderboard | Opt-in, city-wise, monthly reset | P2 |
| One-Tap Repeat | "Donate ₹2,000 again?" for returning donors | P2 |
| Razorpay Route Split | Auto-payout to hosts from donation pool | P2 |
| Notification Service | `donation.captured` → 80G receipt email template | P0 |

---

## 11. Appendix

### A. Competitive Landscape (India)

| Platform | Model | Tax Benefit | Monthly Option | Social Proof |
|----------|-------|-------------|----------------|-------------|
| Ketto | Crowdfunding | 80G | SIP | Donor ticker, backer count |
| GiveIndia | Aggregator | 80G (24hr) | Yes | Employer matching |
| Milaap | Medical/personal | Varies | No | WhatsApp viral |
| Habitat India | Direct | 80G | Monthly builder | Progress bar |
| **Safar Aashray** | **Direct housing** | **80G** | **Aashray SIP** | **Ticker + progress + tiers** |

### B. Preset Amount Rationale

| Amount | Rationale |
|--------|-----------|
| ₹500 | Low barrier entry; coffee-money framing |
| ₹1,000 | Round number; common digital payment amount |
| ₹2,500 | **Anchor** (marked "Most Popular"); week of impact |
| ₹5,000 | Aspirational mid-tier; 2 weeks |
| ₹10,000 | Full month — compelling impact story |
| ₹25,000 | High-value; "3 months = fresh start" narrative |

### C. Files Modified/Created

**Backend (safar-platform)**:
- `services/payment-service/src/main/java/com/safar/payment/entity/Donation.java`
- `services/payment-service/src/main/java/com/safar/payment/entity/DonationStats.java`
- `services/payment-service/src/main/java/com/safar/payment/entity/enums/DonationStatus.java`
- `services/payment-service/src/main/java/com/safar/payment/entity/enums/DonationFrequency.java`
- `services/payment-service/src/main/java/com/safar/payment/repository/DonationRepository.java`
- `services/payment-service/src/main/java/com/safar/payment/repository/DonationStatsRepository.java`
- `services/payment-service/src/main/java/com/safar/payment/dto/CreateDonationRequest.java`
- `services/payment-service/src/main/java/com/safar/payment/dto/DonationOrderResponse.java`
- `services/payment-service/src/main/java/com/safar/payment/dto/VerifyDonationRequest.java`
- `services/payment-service/src/main/java/com/safar/payment/dto/DonationResponse.java`
- `services/payment-service/src/main/java/com/safar/payment/dto/DonationStatsResponse.java`
- `services/payment-service/src/main/java/com/safar/payment/service/DonationService.java`
- `services/payment-service/src/main/java/com/safar/payment/controller/DonationController.java`
- `services/payment-service/src/main/resources/db/migration/V9__donations.sql`
- `services/payment-service/src/main/java/com/safar/payment/config/SecurityConfig.java` (modified)
- `services/api-gateway/src/main/resources/application.yml` (modified)
- `services/api-gateway/src/main/java/com/safar/gateway/filter/JwtAuthFilter.java` (modified)

**Frontend (safar-web)**:
- `app/aashray/donate/page.tsx` (new — 7.65 kB)
- `lib/api.ts` (modified — 3 new API methods)
