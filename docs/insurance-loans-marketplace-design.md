# Insurance & Loans — 3rd-Party Marketplace Design & Implementation

Status: **Sandbox live end-to-end (multi-product + standalone hub).** 2026-06-22.
Provider-agnostic; a real partner activates by adding an enabled adapter + flipping
`insurance.providers.primary`. No real partner creds wired yet (by design).

## Goal
Sell insurance two ways: **embedded** (attached to a flight/stay/rental booking) and
**standalone** (a dedicated "Insurance & Loans" hub, no booking) — plus surface
**loans** (reusing the existing Home-Loan VAS). Commission-based distribution; we
never underwrite (IRDAI: distribute via insurer/aggregator partner).

## What existed (pre-build)
`insurance-service` (port 8097) was a **functional skeleton starved of an enabled
adapter** — Acko + ICICI adapters only threw; travel-only `CoverageType`; no frontend;
`/services/insurance` was a dead link from `CompleteYourTrip`. Booking "micro-insurance"
was a cosmetic fee line, never a real policy.

## Architecture (adapter pattern — mirrors flight/Digio)
`com.safar.insurance.adapter`:
- `InsuranceProviderAdapter` — `quote` / `issue` / `cancel` / `isEnabled`.
- **`SandboxInsuranceProvider`** — NEW, **default, enabled**. Fully-mocked quote/issue/
  cancel for **every** product. Premium + sum-insured are encoded in the quote token so
  `issue()` echoes exact figures. Makes the whole flow real with no creds.
- `AckoInsuranceAdapter` / `ICICILombardInsuranceAdapter` — existing stubs (throw until creds).
- `InsuranceProviderRegistry` — `primary()` from `insurance.providers.primary` (now `SANDBOX`).

## Products (`CoverageType`, broadened)
Travel: `DOMESTIC_TRAVEL`, `INTERNATIONAL_TRAVEL`, `STUDENT_TRAVEL`.
Embedded: `STAY_PROTECTION`, `TENANT_CONTENTS`.
Standalone: `HEALTH`, `LIFE_TERM`, `MOTOR`, `HOME`, `PERSONAL_ACCIDENT`.

## Standalone marketplace (`InsuranceMarketplaceController`, `/api/v1/insurance/marketplace`)
- `GET /products` (public) — catalog: 7 insurance products (buyable) + 4 loan products
  (link to Home-Loan VAS / `/services/loans/*`).
- `POST /quote` (public) — `{coverageType, tenureDays?, ageYears?}` → premium + cover + highlights + quoteId.
- `POST /buy` (auth) — issues a real policy via the registry, reusing
  `InsurancePolicyService.issue` + auto `confirmPayment` (Sandbox), persists ISSUED.
`InsurancePolicy.bookingId` (V2, nullable) distinguishes embedded vs standalone.

## Frontend (safar-web)
- **`/services/insurance`** — NEW hub (fixes the dead link): insurance cards (get-price →
  buy modal → ISSUED), loan cards (→ apply), and *My Policies*. `api.ts`:
  `getInsuranceProducts` / `quoteInsurance` / `buyInsurance` / `getMyInsurancePolicies`.
- **Checkout placeholder** — "🛡️ Add Trip Protection" card on the stay book page → hub.

## Security / gateway
Gateway `JwtAuthFilter` + insurance `SecurityConfig`: `marketplace/products` (GET) +
`marketplace/quote` (POST) public; `buy` authenticated. Route `/api/v1/insurance/**` exists.

## Data model (V2, `insurance` schema)
`insurance_policies.booking_id` (UUID, nullable) + partial index. Reuses existing
travel-shaped columns; standalone policies leave trip fields null.

## Verified (Sandbox, via gateway)
Catalog → 11 products · quote HEALTH → ₹8,000/₹5L · **buy HEALTH → INS-05D2F9C5 ISSUED,
persisted** · travel `/quote` now returns Sandbox results (was empty).

## To go live with a real partner
1. Pick partner (embedded aggregator — Riskcovry/Zopper — or Acko/ICICI direct).
2. Add/finish that adapter (replace the throw-stubs with WebClient calls; the WebClients
   are already configured), set `*_ENABLED=true` + creds.
3. Set `insurance.providers.primary=<PARTNER>` (Sandbox stays as dev fallback).
4. Wire Razorpay order-create + signature verify in `confirmPayment` (currently stores IDs);
   add the provider refund/status webhook (sets `refund_amount_paise`).
5. Embedded: create an `INSURANCE` TripLeg on issue so the existing `LegCancelRouter`
   cancel path activates; add the notification email template (consumer exists).
6. Loans: build out `/services/loans/{personal,business,lap}` apply flows (Home-Loan VAS
   already covers home loans).
