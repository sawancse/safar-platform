# Cake Vendor Onboarding (CAKE_DESIGNER) — Runbook

How to onboard a new cake vendor end-to-end so it sells and shows up in the
storefront. All paths go through the **API gateway** (`http://localhost:8080`
locally, `https://api.bhramankaro.com` live). Backend: **services-service**
(port 8093, schema `services`).

## Roles / auth
- Every vendor step needs the **vendor's JWT** (the listing owner).
- Approval needs an **ADMIN JWT**.
- Dev login: `POST /api/v1/auth/otp/send {phone}` then
  `POST /api/v1/auth/otp/verify {phone, otp:"123456", name?}` (name required for a
  brand-new phone). Returns `accessToken`.

## Status lifecycle
```
DRAFT --submit(KYC gate)--> PENDING_REVIEW --admin approve--> VERIFIED --pause/resume--> PAUSED
```
Only **VERIFIED** listings are publicly visible.

## KYC gate (CAKE_DESIGNER)
Required before `submit`: **AADHAAR, PAN, FSSAI** (FSSAI is legally mandatory for
food sale). Optional: GST. A doc counts if its status is PENDING or VERIFIED
(REJECTED does not). Missing docs → `MissingKycException` on submit.

---

## Steps

### 1. Create the listing (DRAFT)
`POST /api/v1/services/listings`  (vendor JWT)
```json
{
  "serviceType": "CAKE_DESIGNER",
  "businessName": "Priya's Custom Cakes",
  "vendorSlug": "priyas-custom-cakes",      // optional; auto-slugified/uniquified if blank
  "heroImageUrl": "https://.../hero.jpg",
  "tagline": "Artisan wedding & celebration cakes",
  "aboutMd": "Family-run bakery since 2015.",
  "homeCity": "Bangalore",                   // location
  "homePincode": "560038",
  "homeAddress": "123 Baker St, Indiranagar",
  "homeLat": 12.971599, "homeLng": 77.594566,
  "cities": ["Bangalore", "Mysore"],
  "deliveryRadiusKm": 15, "outstationCapable": true,
  "deliveryChannels": ["SELF", "PARTNER"],
  "pricingPattern": "PER_UNIT_TIERED",       // or FLAT_PER_ITEM / PER_TIME_BLOCK / QUOTE_ON_REQUEST
  "pricingFormula": "{\"tiers\":[{\"min_kg\":0,\"max_kg\":1,\"price_per_kg_paise\":500000}]}",
  "calendarMode": "DAY_GRAIN", "defaultLeadTimeHours": 48,
  "cancellationPolicy": "MODERATE",
  "typeAttributes": {                         // cake child (CakeAttributes)
    "bakeryType": "COMMERCIAL",               // HOME_BAKER / COMMERCIAL / CLOUD_KITCHEN
    "ovenCapacityKgPerDay": 50,
    "flavoursOffered": ["Chocolate","Vanilla","Red Velvet"],
    "designStyles": ["Minimal","Elegant"],
    "maxTierCount": 5,
    "egglessCapable": true, "veganCapable": true,
    "deliveryMode": "SELF"                    // SELF / PARTNER / PICKUP_ONLY
  }
}
```
Returns the DRAFT listing with its `id`.

### 2. Upload KYC documents (×3)
`POST /api/v1/services/listings/{id}/kyc-documents`  (vendor JWT) — once per doc:
```json
{ "documentType": "AADHAAR", "documentUrl": "https://.../aadhaar.pdf", "documentNumber": "123456789012" }
{ "documentType": "PAN",     "documentUrl": "https://.../pan.pdf",     "documentNumber": "ABCDE1234F" }
{ "documentType": "FSSAI",   "documentUrl": "https://.../fssai.pdf",   "documentNumber": "11012345001234", "expiresAt": "2027-12-31" }
```
(`documentUrl` is an S3/media-service URL — upload the file there first.)

### 3. Submit for review
`POST /api/v1/services/listings/{id}/submit`  (vendor JWT) → status `PENDING_REVIEW`
(runs the KYC gate).

### 4. Admin approves
`POST /api/v1/services/admin/listings/{id}/approve`  (ADMIN JWT) → status `VERIFIED`.
Also creates a `partner_vendors` stub so booking/assignment flows see the vendor.

### 5. Add products (cakes)
`POST /api/v1/services/listings/{id}/items`  (vendor JWT), one per product:
```json
{
  "title": "3-Tier Chocolate Truffle Cake",
  "heroPhotoUrl": "https://.../truffle-hero.jpg",
  "photos": ["https://.../1.jpg", "https://.../2.jpg"],
  "descriptionMd": "Signature chocolate ganache cake.",
  "basePricePaise": 750000,                  // ₹7,500 (all money in paise)
  "options": { "weights": [{"kg":1,"paise":750000},{"kg":2,"paise":1250000}] },
  "occasionTags": ["WEDDING","BIRTHDAY"],
  "leadTimeHours": 72,
  "displayOrder": 0
}
```
Edit later with `PATCH /api/v1/services/items/{itemId}` (partial — send only changed
fields, e.g. just `heroPhotoUrl`/`photos`). Pause/activate/delete:
`POST .../items/{itemId}/pause | /activate`, `DELETE .../items/{itemId}`.

---

## Verify it's live
- Browse:  `GET /api/v1/services/listings?serviceType=CAKE_DESIGNER&city=Bangalore`
- Storefront API: `GET /api/v1/services/listings/by-slug/{slug}` + `/{id}/items`
- Storefront page: `https://bhramankaro.com/services/storefront/{slug}`
- Browse page: `https://bhramankaro.com/services/cake` → "Cake makers near you" section
  (renders live `browseServiceListings({serviceType:'CAKE_DESIGNER'})`).

## Material vs non-material edits (on a VERIFIED listing)
- **Material** (businessName, vendorSlug, pricingPattern, pricingFormula, homeCity,
  homeAddress, homePincode) → staged to `pending_changes`, needs admin re-approval.
- **Non-material** (heroImageUrl, tagline, aboutMd, photos, type attrs) → applies
  immediately.

## Notes / gotchas
- `serviceType` is the JOINED-inheritance `@DiscriminatorColumn`; the create response
  now carries it (fix `4b684ba`).
- Gateway routes cover `/listings/**`, `/items/**`, `/admin/**`, `/invites/**`
  (items + invites routes added in `4b684ba`).
- Partial item PATCH validates only fields sent (fix `626f3ff`).
- Alternative onboarding: admin BD invite — `POST /api/v1/services/admin/invites`
  returns a token deep-link; vendor opens `/api/v1/services/invites/{token}` (public)
  then runs the wizard.
