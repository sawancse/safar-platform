# Insurance Marketplace v2 — PolicyBazaar + PhonePe-grade Design (BMAD)

> Status: DESIGN → IN IMPLEMENTATION · Owner: BhramanKaro · Date: 2026-06-23
> Supersedes the MVP in `insurance-loans-marketplace-design.md` for the marketplace leg.
> Method: BMAD (analyst brief → competitive benchmark → decomposition → architecture → phased build).

## 1. Goal & framing
Turn the current single-quote sandbox marketplace into a **PolicyBazaar-class compare-and-buy
marketplace** with a **PhonePe-class embedded, low-friction distribution layer**, on a
**provider-agnostic aggregator** backend so go-live = dropping in a partner's API keys.

We are a **distribution partner (corporate agent / broker)** — we do **not** underwrite. Revenue =
commission per policy + assisted-sales conversions. Until a partner signs, the **Sandbox underwriter**
serves realistic multi-insurer demo data (every plan flagged `sandbox=true`); **no real charge** is
taken for a sandbox policy (that would be fraud).

## 2. Competitive benchmark — what we adopt
| Dimension | PolicyBazaar | PhonePe Insurance | BhramanKaro adopts |
|---|---|---|---|
| Core model | Compare 50+ insurers, deep journeys, advisor-led | Few curated insurers, 2-tap embedded buy, bite-size | **Both**: PB compare depth + PhonePe embedded simplicity |
| Discovery | Product hubs + compare grid | Inline cards in a super-app | Product hub `/services/insurance` + compare grid + embedded checkout opt-ins |
| Quote | Long product-specific forms | Pre-filled from profile, minimal | Product-specific journeys, but pre-fill from user profile (PhonePe trick) |
| Compare | Side-by-side grid, sort/filter, claim ratio, cashless count, add-ons | Minimal | **PB-style compare grid** = flagship |
| Trust | Claim-settlement ratio, IDV, reviews | UPI/brand trust | Claim ratio + insurer rating + cashless count + "Demo" ribbon in sandbox |
| Assisted sales | Heavy tele-advisor | Light | **Talk-to-advisor callback** lead capture |
| Lifecycle | Renewals, claims concierge | Reminders | Renewal reminders + claims-assistance hub |
| Monetization | Commission + leads | Commission | Commission via aggregator; leads via advisor callback |

## 3. Personas & journeys
- **Self-serve comparer (PB)**: picks product → fills journey form → compares plans → tweaks add-ons → buys online.
- **Embedded impulse buyer (PhonePe)**: at stay/flight checkout toggles a 1-line cover (e.g. ₹149 trip protection) → folded into one payment.
- **Assisted buyer**: overwhelmed → "Talk to an advisor" → callback lead → ops-assisted close.
- **Existing policyholder**: gets renewal reminder + claims help from "My Policies".

## 4. Feature decomposition (4 capabilities × 4 priority products)
Products in scope: **Health, Term life, Motor, Travel** (Home/PA/Tenant/Stay remain on the generic path).

### 4.1 Multi-insurer compare grid (flagship)
- Backend `POST /marketplace/compare` → `{quoteId, plans:[PlanOption]}` (all insurers/tiers).
- `PlanOption`: insurer, planName, premium, sumInsured, features[], **claimSettlementRatio**,
  **cashlessCount**, **insurerRating**, **addOns[]**, wordingUrl, recommended, **sandbox**.
- Frontend: comparison grid, sort (premium / claim ratio / cover), filter, select-to-buy, "compare up to 3".

### 4.2 Product-specific quote journeys
- **Health**: members + ages, city/pincode, pre-existing conditions, sum-insured selector.
- **Term**: age, gender, smoker, annual income, cover amount, tenure.
- **Motor**: registration no, make/model, manufacturing year, IDV, prev-claim.
- **Travel**: destination(s), trip dates, traveller ages, single vs multi-trip.
- Pattern: a `ProductQuoteForm` per product → maps to the common `compare` request + `productParams` map.

### 4.3 Add-ons / riders & plan detail
- Each plan exposes selectable `addOns[{code,label,premiumPaise}]`.
- Selecting add-ons recomputes total = base + Σ add-ons (sandbox: re-encode the quote token premium).
- Plan-detail surface: full inclusions/exclusions, claim ratio, cashless network, waiting period.

### 4.4 Talk-to-advisor + lifecycle
- `POST /marketplace/advisor-callback` (public) → persist `insurance_lead` + Kafka `insurance.lead.created`
  → notification (WhatsApp/email to ops) + admin queue.
- Renewal reminders: `RenewalReminderScheduler` finds policies expiring in T-30/T-7 → Kafka → email.
- Claims-assistance hub page + "raise a claim" lead.

## 5. Architecture
```
                       ┌──────────────── insurance-service (8097) ────────────────┐
 web /services/insurance│  InsuranceMarketplaceController                          │
   ├ compare grid  ─────┼─► POST /marketplace/compare ─► registry.primary()        │
   ├ product forms      │        .comparePlans(req) ─► [SANDBOX | AGGREGATOR]      │
   ├ add-ons/detail     │  POST /marketplace/buy (planId + addOnCodes)            │
   ├ advisor callback ──┼─► POST /marketplace/advisor-callback ─► insurance_lead   │
   └ my policies        │  GET  /certificate/{ref}  (public, cert email target)    │
 embedded checkout  ────┼─► /marketplace/quote (1-line) + booking-total fold-in    │
                        │  RenewalReminderScheduler ─► Kafka insurance.policy.*     │
                        └──────────────┬───────────────────────────────────────────┘
                                       │ Kafka: insurance.policy.issued / .cancelled / .lead.created / .renewal-due
                              notification-service ─► cert email + advisor + renewal
```
- **Provider abstraction unchanged**: `InsuranceProviderAdapter` gains `comparePlans()` (default wraps single `quote()`).
- **AggregatorInsuranceAdapter** (PhonePe/Riskcovry-class) implements real `/v1/quotes|policies|cancel`,
  gated by `aggregator.enabled` + creds. Sandbox supplies multi-plan demo data until then.

## 6. Data model (insurance schema)
- `insurance_policies` (exists) — add nothing required for compare (stateless quotes).
- **NEW `insurance_leads`** (V3): id, user_id?, name, phone, email?, product, coverage_type?,
  city?, preferred_time?, notes?, status (NEW→CONTACTED→CONVERTED→LOST), source, created_at.
- Compare/quote stays **stateless** — the bindable plan lives in the opaque `quoteId` token.

## 7. API contracts (additive, all under `/api/v1/insurance/marketplace`)
- `POST /compare` (public) → `{quoteId, plans:[PlanOption]}`; body `{coverageType, productParams{}, tenureDays?, ageYears?, members?}`.
- `POST /buy` (auth) → extend with `planId/quoteId` + `addOnCodes[]`.
- `POST /advisor-callback` (public) → `{leadId}`.
- `GET /products` (exists), `POST /quote` (exists, embedded), `GET /certificate/{ref}` (done).

## 8. Phased roadmap
- **P1 (in progress)**: `PlanOption` model, Sandbox multi-plan, adapter `comparePlans`, `/compare`, buy-with-plan+add-ons. ✅ backend started.
- **P2**: Frontend compare grid + sort/filter + add-on selection + 4 product quote forms.
- **P3**: Advisor callback (V3 + entity + endpoint + Kafka + notification + admin queue).
- **P4**: Renewal reminders scheduler + claims-assistance hub.
- **P5 (go-live)**: flip `aggregator.enabled` + creds + `INSURANCE_PRIMARY_PROVIDER=AGGREGATOR`;
  real Razorpay (only with a real underwriter); refund webhook.

## 9. Compliance / risk
- **No real charge** for sandbox policies (label "Demo"/sandbox; Razorpay test-mode only) until a real
  IRDAI-registered partner is live. Real Razorpay flips on with the underwriter.
- PII: certificate link is policyRef-gated (random ref). Leads store phone/email — minimal retention.
- Insurer names in sandbox are demo stand-ins; the live aggregator supplies authoritative data.

## 10. Open decisions
- Aggregator partner choice (Riskcovry vs Zopper vs PB Partners) — affects field mapping only.
- Commission accounting: per-policy ledger vs settlement report (defer to payments leg).
- Advisor routing: round-robin vs single ops inbox (start single inbox).
