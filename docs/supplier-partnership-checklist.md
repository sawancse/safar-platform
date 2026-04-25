# Supplier Partnership Application Checklist

**Purpose:** Get Udaan, FernsNPetals B2B, and Amazon Business onto the platform as integrated suppliers. Use this before each application call so you don't get stalled mid-way.

**Status:** Pre-application. None of these are signed yet.

---

## 0. Prerequisites (one-time, do these BEFORE applying anywhere)

### 0.1 Entity readiness
- [ ] GST certificate of registered Safar entity (need ARN + GSTIN)
- [ ] PAN of the entity
- [ ] Certificate of Incorporation
- [ ] Latest audited or provisional balance sheet (most B2B suppliers ask)
- [ ] Cancelled cheque OR bank verification letter
- [ ] Authorised signatory KYC (PAN + Aadhaar)
- [ ] Board resolution authorising the partnership signing (if pvt ltd)

### 0.2 Operational readiness
- [ ] Drop-shipping address(es) finalised — supplier ships to your warehouse OR direct to vendor (the latter is harder; needs supplier support)
- [ ] Decision: single virtual warehouse OR per-city. Phase 1 PRD says single — confirm before signing.
- [ ] GSTIN per state where you want to receive goods (cross-state shipments incur IGST; same-state CGST+SGST)
- [ ] Receiving SOP — who counts the truck, who signs invoice, who marks PO delivered in admin
- [ ] Defective-goods return SOP per supplier (FNP doesn't accept floral returns; Udaan has 24hr window for some categories)

### 0.3 Volume forecast (suppliers ask)
- [ ] 90-day BOM forecast — flour, sugar, butter, eggs, fondant, marigolds, balloons (rough is fine; they use it for credit terms)
- [ ] Average daily order count + ticket size
- [ ] City breakdown (some suppliers price differently per zone)

---

## 1. Udaan — Wholesale Groceries

**What you get:** ~80% of cook BOM (atta, sugar, oil, masalas, packaged), pan-India delivery, COD or NEFT, 1-3 day lead time depending on city.

**Application path:**
1. Go to `https://udaan.com/business/seller` → "Become a buyer / partner" (NOT seller; you're buying)
2. Click "Register your business" — needs GST + PAN + business address proof
3. KYC verification: 2-3 business days (they call to verify office address)
4. Account Manager assigned after first ₹50K of orders (rough threshold)

**Documents to upload:**
- [ ] GST certificate
- [ ] PAN card
- [ ] Address proof (rent agreement / electricity bill ≤ 3 months old)
- [ ] Cancelled cheque
- [ ] Authorised signatory KYC

**API access — separate ask:**
- [ ] Email `partners@udaan.com` after first ₹2L of organic orders, asking for "API access for ERP/inventory integration"
- [ ] They send a partner-API NDA (sign + return)
- [ ] Get sandbox credentials: 5-7 business days
- [ ] Sandbox endpoints: catalog sync, order placement, order status, invoice
- [ ] Production credentials require: signed MSA + minimum ₹10L/month commitment OR explicit waiver from your AM
- [ ] Webhook URL: provide a Safar endpoint (`/api/v1/internal/udaan/webhook`) — they'll POST order status updates

**Things to negotiate:**
- [ ] Credit terms (NET_15 default; push for NET_30 if volume justifies)
- [ ] Return window (default 24h; ask for 48h on perishables)
- [ ] Damage tolerance (default 0.5% absorbed; ask for 1% on flour/sugar)
- [ ] Pricing tier (volume-based; ask for next tier above your forecast to motivate)
- [ ] City-pricing parity (ask for same price across all metros where you operate)

**Red flags / watch-outs:**
- Udaan B2B prices change daily — your `last_unit_cost_paise` will drift; build a daily catalog-sync job
- Some SKUs are marketplace (3rd-party seller) vs Udaan-direct — only direct has reliable API
- Their COD has a ₹1L per-order ceiling

---

## 2. FernsNPetals B2B — Decor & Florals

**What you get:** Marigold garlands, rose loose, balloons, candles, ribbons, themed decor kits. Pan-India same-day in 100+ cities.

**Application path:**
1. Go to `https://fnp.com/franchise` → enquire — they don't have a public B2B portal, it's relationship-led
2. Initial call with their B2B team within 48h of enquiry
3. Sign NDA → share volume forecast → quote → MSA
4. Onboarding: 2-3 weeks

**Documents:**
- [ ] GST + PAN + address proof (same as Udaan)
- [ ] Sample BOM (give them: 30 marigold garlands/month, 50 rose loose kg, 200 balloons/month — adjust to your real forecast)
- [ ] Brand guidelines if you want their delivery boxes co-branded

**Two integration models — pick one:**

**Model A — White-label / branded shipment** (recommended)
- FNP ships to event venue with neutral packaging (your Safar branding optional)
- They handle florist + delivery
- You handle customer relationship + Safar admin assigns vendor
- API: order-placement + tracking only; no catalog API needed (you mirror their decor SKU list)
- Cost: 10-15% above retail FNP price

**Model B — Bulk to your warehouse**
- FNP delivers daily/weekly bulk to a Safar warehouse
- You repackage + dispatch to vendors/events
- Cheaper per unit but you take on logistics + spoilage
- Only worth it at >₹5L/month

**API access:**
- [ ] Their B2B API is REST + Bearer-token, partner-only
- [ ] Endpoints: place order, cancel, track delivery, list catalog
- [ ] Sandbox: ~1 week after MSA
- [ ] Webhook for delivery confirmation

**Things to negotiate:**
- [ ] Lead time (default 24h; ask for 12h same-day)
- [ ] Substitution policy (if marigold unavailable, sub with similar at no extra cost?)
- [ ] Damage on floral (industry standard: no returns, but ask for replacement on photo evidence)
- [ ] Branded co-shipment (they pack with Safar leaflet)

**Red flags:**
- Floral SKUs have seasonal price swings — don't assume catalog price is stable beyond 7 days
- Tier-2/3 cities have unreliable same-day SLAs; verify per city you operate in
- They have minimum order value ₹2K per order in some cities

---

## 3. Amazon Business — Long-tail SKUs

**What you get:** Anything not on Udaan/FNP — appliances (induction, blenders), packaging (boxes, bags), specialty items, GST input credit.

**Application path:**
1. `business.amazon.in` → "Register your business"
2. GST + PAN → verified in 24h (Amazon is fastest)
3. Account Manager only after ₹5L cumulative spend; until then self-serve
4. Bulk pricing tab unlocks at ₹50K cumulative spend

**Documents:** All standard (GST, PAN, address). Easiest of the three.

**API access:**
- [ ] Amazon Business has Marketplace Web Service (MWS) — get keys after registration
- [ ] Selling Partner API (SP-API) requires "Solution Provider" role; harder to get for buyer use
- [ ] Easier path: use Amazon Business browser-based ordering; only integrate via API for high-volume SKUs
- [ ] If you go API: AWS IAM role + SP-API credentials → 7-10 day approval

**What to integrate:**
- Bulk price retrieval per ASIN
- Place order with PO reference
- Order status webhooks via SNS

**Things to leverage:**
- [ ] GST input credit auto-categorised (Amazon Business does this for you)
- [ ] Multi-buyer accounts — different admins can place orders, all consolidated for billing
- [ ] Quantity-discount Bulk Buy pricing

**Red flags:**
- API is over-engineered for our scale — manual ordering may be fine until ₹2L/month
- Some SKUs are 3rd-party (FBA) — your delivery SLA is at the seller's mercy

---

## 4. Decision matrix — sequencing

| Phase | Supplier | Integration depth | Trigger to start |
|---|---|---|---|
| Now | All three — accept manual orders | Onboard as `integration_type=MANUAL` rows | After your first 10 cake/decor orders |
| Phase 2a (~3 mo) | Udaan API | Catalog sync (pull) + order placement | After ₹2L/mo Udaan organic spend |
| Phase 2b (~4 mo) | FNP API | Order placement + tracking webhook | After signed MSA |
| Phase 3 (~6+ mo) | Amazon Business API | Long-tail bulk only | If long-tail BOM exceeds 20 SKUs |

---

## 5. Internal sign-offs needed

- [ ] Founder/CEO sign on MSA before signing
- [ ] Finance — approves payment terms + credit limit per supplier
- [ ] Legal — reviews indemnity clauses (typical: supplier indemnifies for product safety; you indemnify for non-payment)
- [ ] Tech — reviews API security (key rotation policy, IP whitelisting if available)
- [ ] Ops — sets up receiving SOP per supplier

---

## 6. Tracking template

Save partner contracts in `safar-platform/contracts/suppliers/<supplier>/` (gitignored or encrypted). Don't commit MSA PDFs to repo.

For each supplier track:
- MSA signed date + version
- API credentials location (1Password vault path)
- Account Manager contact
- Last catalog sync date
- Outstanding payment cycles
- Issue log (defects, delays, billing disputes)

---

## 7. What to NOT promise

- Don't commit to a minimum monthly spend you can't hit — suppliers retract preferential pricing
- Don't sign exclusivity for any category — keeps you free to pick the better deal
- Don't agree to consignment-stock model on day one (supplier ships to your warehouse, you pay only on consumption) — needs WMS infrastructure you don't have yet
- Don't share customer PII — FNP especially asks for delivery names; share only event-day address + recipient name, not contact backstory

---

## Appendix: Application email templates

### A.1 Udaan partner enquiry
> Subject: B2B partnership — Safar (cooks + event services marketplace)
>
> Hi Udaan team,
>
> We operate Safar — a marketplace for cooks and event services across India (Bangalore + 4 metros, scaling). Our cook + cake businesses procure ~₹X/month of staples (atta, sugar, oil, masalas) and we'd like to explore a B2B account with API integration for our procurement system.
>
> 90-day BOM forecast attached. We'd like to discuss credit terms, city coverage, and partner-API access.
>
> Founder name • Designation • Phone

### A.2 FernsNPetals B2B enquiry
> Subject: B2B partnership for event decor — Safar Celebrations
>
> Hi FNP team,
>
> Safar runs an event marketplace for cake/decor/pandit/music/staff in 5 metros. Our decor vertical orders marigold garlands, roses, balloons and themed kits at scale (~Y orders/month). We'd like to discuss a B2B partnership with white-label shipment OR bulk-to-warehouse model.
>
> Happy to set up a 20-min call this week.
>
> Founder name • Designation • Phone
