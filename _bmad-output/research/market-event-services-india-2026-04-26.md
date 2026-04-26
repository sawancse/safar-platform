# Event-Services Market Research — Competitive Teardown
**Date:** 2026-04-26
**Purpose:** Inform Safar's services-leg redesign (vendor self-onboarding + booking↔catalog deep-link + provider taxonomy)
**Method:** WebSearch breadth scan across 14 platforms (India-first + global comparators). WebFetch was blocked in this environment, so detail comes from search-result snippets, vendor handbooks, and FAQ pages rather than direct DOM walking. Where a flow could not be confirmed, it is noted "[unverified]".

---

## TL;DR — top 5 insights for Safar's design

1. **Vendor profile is the listing; "items" are an optional second tier.** [Goal C] WedMeGood, ShaadiSaga, WeddingWire and The Knot all use a flat `/profile/{vendor-slug-id}` or `/vendors/{city}/{category}/` URL pattern. The vendor *is* the SKU. Items/packages (e.g. "1-day photography package", "3-day pre-wedding package") are surfaced as filters and as price tiers *inside* the profile, not as separate URLs. **Implication:** Safar's table-per-type with optional `service_items` is correctly aligned with the dominant pattern. Use `/services/{category}/{vendor-slug}` for the canonical URL; items live as anchors/tabs within it.

2. **Self-onboarding is universal but Approve gate is the norm in India.** [Goal A] WedMeGood, ShaadiSaga (WeddingBazaar Partners app), Sulekha, JustDial all let vendors self-register via web or dedicated "for Business" mobile app. None go fully instant — every platform has a verification step (KYC/document check) before the listing goes live. JustDial gates its "JD Trust/Verified" badge on KYC + 3.8★ rating. **Implication:** Safar's "vendor publishes → admin approves" flow is the standard. The differentiator is *speed-to-approval* (target 24h SLA), not skipping approval.

3. **WhatsApp + in-app chat + phone are all required, not either-or.** [Goal B] WedMeGood explicitly documents Call / SMS / WhatsApp / Email / in-app messaging as parallel channels. Indian vendors will not abandon WhatsApp, so the booking deep-link must coexist with a "Chat on WhatsApp" CTA on the booking row. **Implication:** Safar's booking detail page should render: (1) deep-link to the ordered service item, (2) deep-link to the vendor profile, (3) a WhatsApp `wa.me/{phone}?text=...` button pre-filled with booking ID.

4. **Trust stack in India is layered: KYC + license + reviews + tenure + badge.** [Goal A] Practo verifies MCI registration; Bakingo/home-bakers need FSSAI; Urban Company requires gov ID + police verification + skill assessment + on-arrival selfie matching; JustDial needs KYC + rating. No single signal carries the load. **Implication:** Safar's services-service should expose a `verifications` JSON column (or sub-table) with per-type license fields — `fssai_license_no` for cake/cook, `police_verification_status` for staff/pandit, `gst_no` everywhere, `pan` everywhere — and surface them as discrete badges.

5. **Pricing model varies by vertical — do NOT force one schema.** [Goal C/D] Cake = per-kg. Photography = per-day-package. Pandit = per-event flat. Decor = quote-on-request. Singer = per-hour. Caterer = per-plate. Fiverr's "Standardized Gig packages" model (Basic/Standard/Premium tier with category-mandatory fields) is the cleanest abstraction. **Implication:** Each provider-type table should declare its own `pricing_unit` (KG/HOUR/EVENT/DAY/PLATE/QUOTE) and its own price-tier shape. Don't try to unify under `service_items` — let each type table own its pricing semantics.

---

## Comparison matrix

| Platform | 1. Onboarding | 2. Listing model | 3. Booking | 4. Trust signals | 5. Search facets | 6. Pricing | 7. Multi-vertical | 8. Novel |
|---|---|---|---|---|---|---|---|---|
| **WedMeGood** | Self-serve web + "For Business" mobile app; admin verifies; free listing | Vendor-as-listing; `/profile/{name-id}` and `/vendors/{city}/{category}/` | Request-for-quote via inquiry form, in-app chat, call, WhatsApp; subscription model (vendor pays monthly) | Certified Reviews (cross-vendor verified), real-time inquiry alerts, response timing | City × category × price; 20+ categories incl. cake, pandit, decor, mehndi, DJ | Vendor sets price details on profile; not standardized | Single shared platform across 20+ verticals | Cross-vendor "Certified Reviews" — review only counts if guest also reviewed 2+ other vendors from their wedding |
| **ShaadiSaga / WeddingBazaar** | Self-serve via "WeddingBazaar Partners" app; vetted; now part of Matrimony.com | Vendor-as-listing across 18 categories | Send requirements to multiple vendors → compare quotes → book through portal | Reviews, distribution into Matrimony.com 7M+ DB | Category × city × budget; package types (1-day, 3-day, pre-wedding) | Quote-driven, package-based for photographers | 18 verticals on one platform | Multi-vendor RFQ broadcast: one form goes to many vendors at once |
| **BookEventz** | Listed via tie-ups (admin-curated leaning); vendors pay for marketing | Hybrid: venues are first-class (`/banquets/{city}/wedding`) + vendors as second tier | Real-time availability for venues; virtual tour; user fills detailed requirements form | Reviews, capacity, food policy displayed | City × event-type × capacity × food policy | Venue: per-plate + venue rental; vendors: package-based | Venues primary, vendors secondary | Virtual tour + real-time availability for banquets |
| **VenueMonk** | Admin-curated (concierge model); vendors pay for listing | Venue-as-listing; `/{city}/wedding-venues` | Pay booking amount to VenueMonk OR direct to venue; "lowest-price guarantee" up to 30% off | Negotiated rates, lowest-price guarantee | City × event-type | Negotiated/discounted | Venue-only, vendors lightly | Concierge takes 30% off via aggregation power; payment can flow through marketplace OR direct |
| **Bakingo** | Not a marketplace — vertical-integrated brand | Item-as-listing (cake catalog); URL `/cakes/{city}`, `/birthday-cakes/{city}`, `/designer-cakes` | Instant book; pick weight, flavor, delivery slot (same-day / midnight / instant) | Brand-level only (no per-baker reviews — Bakingo *is* the supplier) | City × occasion × flavor × type (jar/photo/poster/fondant) × weight | Per-kg fixed price | Single vertical (cake-only) | "Instant" 60-min delivery + midnight slot + add-on gifts at checkout |
| **FNP** | Not a marketplace (D2C + 320 owned stores) | Item-as-listing (occasion-driven catalog) + service add-ons (decor, custom cake) | Instant book; same-day / 60-min / fixed-time / midnight slots | Brand reputation, 320 stores | Occasion × product type × city PIN (19,000+ codes) | Fixed catalog price; quote for decor | Cake + flowers + decor + experiences under one brand | Occasion-led IA: "Birthday", "Anniversary", "Rakhi" verticals navigated by *event* not by *product* |
| **Practo** | Self-serve via "Practo Profile" + Practo Ray; doctor submits gov ID, MCI reg #, degree | Doctor-as-listing | Instant book or 60-second connect for online consult | MCI verification cross-checked against state councils, "Bluebook" specialization verifier, only verified-patient reviews, manual review moderation | Specialty × city × locality × language × consult type (in-person/online) | Per-consult fixed | Single vertical (healthcare) | "Bluebook" — internal mapping that ties degree → permitted specializations to prevent over-claiming |
| **Urban Company** | Self-serve "Partner" app onboarding → 3-45 day training → gov ID + police verification + interview + skill test → on-arrival selfie match | Service-item-as-listing (catalog model — vendor is hidden behind UC brand initially) | Instant book; UC routes job to nearest verified pro | Gov ID, police verification, skill assessment, on-arrival selfie facial-match, insurance, SOS button, refund/re-service guarantee | Service category × time slot × city | Fixed catalog price set by UC, not pro | Catalog spans 30+ services; pro is matched, not chosen | "Filtration funnel, not signup funnel" — Urban Company explicitly treats onboarding as a filter; ~20% acceptance rate. On-arrival selfie facial-match is the standout trust feature |
| **Sulekha** | Self-serve "List Your Business"; verify documents → free trial → paid recharge | Vendor-as-listing; 2000+ categories | Pay-per-lead model; user fills form → leads forwarded to matching providers | Document verification + reviews | 2000+ categories × city × need | Vendor sets own; pay-per-lead to Sulekha | Massive multi-vertical (home/life/self) | Pay-per-lead vs flat-listing: Sulekha monetizes the *connection*, not the listing |
| **JustDial** | Self-serve "List Free" via OTP; KYC + ratings unlock badges | Business-as-listing | Phone-call routing primary; "JD Mart" for B2B | "JD Trust", "JD Verified" badges (gated on KYC + ≥3.8★), "JDRR Certificate", Biz Boosters paid add-ons | Business name × city × category (huge taxonomy) | Vendor-stated; JustDial doesn't transact | Universal directory | Trust badge tied to *both* KYC AND minimum review score — single signal isn't enough to claim verified |
| **Housejoy** | Self-serve partner registration + gov ID + local + permanent address proof + interview + reference + skill assessment | Service-as-listing (catalog) | Auto-allocation of orders to partners; in-app payment | Gov ID, address proofs, references, skill assessment | Category × city | Fixed catalog price | 8+ verticals (home, beauty, mover, health) | Auto-allocation = customer doesn't pick the pro |
| **The Knot** (US) | Self-serve via "WeddingPro"; storefront listing | Vendor-as-listing across 25 categories | Inquiry-based; vendor responds with quote | 3M+ reviews, response rate, "newlywed reviews" aggregated | 25 categories × city × style; date-availability filter | Vendor-set tiers | Single platform, 25 verticals | Date-availability filter (filter to vendors free on your wedding date) |
| **Zola** (US) | Self-serve; free listing + paid placement upgrades | Vendor-as-listing storefront | Pay-to-connect: vendor pays credits per vetted lead; budget tracker integrates payments | Reviews, vetted-lead system, organic ranking (not pay-to-rank) | Category × style × date-available × price | Tiered packages | Wedding-only verticals | "Pay-to-connect" — vendor only spends when a real lead converts to conversation, not for impressions |
| **Etsy** | Self-serve shop wizard; multi-session save-and-resume; progress bar | Item-as-listing (shop = container of items) | Instant buy; per-item cart | Star Seller, reviews, response time | Category attributes (material, color, occasion, holiday, height, width); category determines available attributes | Per-item; sellers set own | Single mega-category structure | Category-driven attributes: choosing category dynamically loads the right filter fields. Bulk-create-by-category UX |
| **Fiverr** | Self-serve; gig-creation wizard; standardized packages enforced | Gig-as-listing with mandatory Basic/Standard/Premium tiers; seller is secondary | Instant book the gig package + extras at checkout | Seller level (New/Level 1/2/Top Rated), response time, completion rate, reviews | Category × budget × delivery-time × seller-level | Packages: Basic/Std/Premium starting $5, with mandatory category-defined elements + Extras | Single platform, 24+ categories | "Standardized Gig packages": each category defines mandatory fields each gig must populate. Forces apples-to-apples comparison |

---

## Platform deep-dives

### WedMeGood
- **Onboarding:** `/vendor-register` and `/vendor-signup` web routes; "WedMeGood for Business" iOS+Android app. Vendor provides email + password, then category, city, price details. Vendors must hold "internal as well as governmental and statutory licenses" per ToS. Listing free; revenue model is monthly subscription for prominence + ₹500/₹1500 user-side service fees.
- **Listing:** Vendor-is-listing. Two URL shapes seen: `/profile/{Slug}-{numericId}` (vendor profile) and `/vendors/{city}/{category}/` (city+category index). Sub-pages: `/profile/{...}/portfolio`. ~1 lakh+ vendors, 200+ cities.
- **Booking:** Inquiry-based (no instant book). Vendor receives real-time alerts via "WedMeGood for Business" app and can respond by Call / SMS / WhatsApp / Email / in-app message — all five channels supported in parallel.
- **Trust:** "Certified Reviews" — a review only counts as Certified if the same client also reviewed ≥2 other vendors from their wedding (cross-vendor consistency proof). Response timing surfaced.
- **Search facets:** 20+ categories (photographer, decorator, pandit, cake, mehndi, makeup, planner, choreographer, DJ, catering, invitations, favors, jewellery, bridal/groom wear, pre-wedding shoot, planner, virtual planner, food stalls, bartender). City × category × price.
- **Pricing:** Vendor sets price details on profile; no standardized package shape across categories.
- **Novel:** Cross-vendor Certified Review system; 5-channel parallel inquiry routing.
- **Sources:** [vendor-register](https://www.wedmegood.com/vendor-register), [vendor-signup](https://www.wedmegood.com/vendor-signup), [terms_vendor](https://www.wedmegood.com/terms_vendor), [vendors index](https://www.wedmegood.com/vendors/), [Certified Reviews blog](https://www.wedmegood.com/blog/looking-to-enhance-your-profiles-credibility-heres-how-reviews-on-your-profile-can-now-be-certified/), [Business app](https://play.google.com/store/apps/details?id=com.wedmegood.vendor)

### ShaadiSaga / WeddingBazaar
- **Onboarding:** Self-serve via "WeddingBazaar Partners" app (rebranded from "ShaadiSaga Vendors"). Now part of Matrimony.com.
- **Listing:** Vendor-as-listing across 18 categories: Photographer, Bridal Makeup, Decorator, Mehndi, Invitation, Choreographer, Videographer, Venue, Bridal Designer, Planner, Caterer, Jewellery, Honeymoon, Entertainment, DJs, Pandits, Gifts, Cars.
- **Booking:** Multi-vendor RFQ — user sends one requirement form to many vendors, compares quotes, books. 6,000+ trusted vendors. Reach claims 7M+ Matrimony.com users + 20M+ web/social.
- **Trust:** Vetted vendors; reviews; package filters (1-day / 3-day / pre-wedding).
- **Search facets:** Category × city × budget × package type.
- **Pricing:** Quote-driven; package-based for some categories (esp. photography).
- **Novel:** Distribution arbitrage via Matrimony.com cross-sell.
- **Sources:** [WeddingBazaar.com](https://www.weddingbazaar.com/), [Vendors app](https://play.google.com/store/apps/details?id=com.shaadisaga.vendorapp)

### BookEventz
- **Onboarding:** Tie-up driven (admin-curated leaning); vendors pay for marketing.
- **Listing:** Hybrid — venues primary (`/banquets/{city}/wedding`), vendors secondary. 262 venues seen on Vadodara page alone.
- **Booking:** User fills detailed requirement → platform suggests best-suited venues + vendors. Real-time availability + virtual tour for venues.
- **Trust:** Reviews, capacity, food policy, photos.
- **Search facets:** City × event-type × capacity × food policy × price.
- **Pricing:** Per-plate + venue rental; per-package for vendors.
- **Novel:** Virtual tour + real-time availability widgets for venues.
- **Sources:** [bookeventz.com](https://www.bookeventz.com/), [Mumbai event-planner](https://www.bookeventz.com/event-planner/mumbai)

### VenueMonk
- **Onboarding:** Admin-curated. Vendors negotiated with manually.
- **Listing:** Venue-as-listing; `/{city}/wedding-venues` URL shape.
- **Booking:** "Pay booking amount to VenueMonk OR directly to the venue" — flexible payment; once paid, "100% confirmed" booking.
- **Trust:** Lowest-price guarantee (up to 30% off); negotiated rates.
- **Search facets:** City × event-type (wedding/reception/engagement/pre-wedding).
- **Pricing:** Negotiated/discounted vs market.
- **Novel:** Marketplace can be a payment intermediary OR step out — customer's choice. Useful pattern for India where vendors may resist gateway fees.
- **Sources:** [venuemonk.com](https://www.venuemonk.com/), [why-venuemonk](https://www.venuemonk.com/why-venuemonk), [FAQs](https://www.venuemonk.com/faqs)

### Bakingo
- **Onboarding:** N/A — vertically integrated; Bakingo *is* the baker. No third-party seller flow.
- **Listing:** Item-as-listing. URL pattern: `/cakes`, `/cakes/{city}`, `/birthday-cakes/{city}`, `/designer-cakes`, `/best-seller`, `/cake-stores-in-{city}`.
- **Booking:** Instant. Pick weight + flavor + delivery slot. 3 slot types: same-day / midnight / instant (60-min in metros).
- **Trust:** Brand only (no per-baker because there are no third-party bakers).
- **Search facets:** City × occasion × flavor (12: chocolate, fruit, eggless, black forest, coffee, butterscotch, strawberry, vanilla, pineapple, red velvet, mango, blueberry, ferrero, kit-kat, oreo) × type (jar / cupcake / photo / poster / fondant / vegan / kit-kat / heart-shaped / 5-star / pastry) × weight.
- **Pricing:** Per-kg, fixed.
- **Novel:** Item catalog + city overlay + occasion overlay = 3-axis IA. Add-on gifts in cart.
- **Sources:** [bakingo.com](https://www.bakingo.com/), [/cakes/delhi](https://www.bakingo.com/cakes/delhi), [/designer-cakes](https://www.bakingo.com/designer-cakes), [/best-seller](https://www.bakingo.com/best-seller)

### FNP (Ferns N Petals)
- **Onboarding:** N/A (D2C + 320 owned stores, since 1994).
- **Listing:** Item-as-listing within occasion verticals. 19,000+ PIN codes.
- **Booking:** Instant. 60-min delivery in metros for cakes; same-day / morning / midnight / fixed-time slots.
- **Trust:** Brand reputation, owned-store presence.
- **Search facets:** Occasion (Birthday, Anniversary, Valentine's, Rakhi, Diwali, Christmas) × product type (flowers, cakes, plants, personalized, decor, experiences) × city × PIN.
- **Pricing:** Fixed for catalog; quote-based for event décor.
- **Novel:** **Occasion-led navigation** — the primary IA isn't "products" but "what are you celebrating?" That maps directly to Safar's event-services use case.
- **Sources:** [fnp.com](https://www.fnp.com/), [Mumbai stores](https://www.fnp.com/info/mumbai-fnp-stores), [international](https://www.fnp.com/info/international)

### Practo
- **Onboarding:** Self-serve via Practo Ray. Doctor uploads gov-issued photo ID + MCI registration number + medical degree copies. Practo verifies registration against state medical council databases. Specialization claim cross-checked against the internal "Bluebook" mapping.
- **Listing:** Doctor-as-listing. 100,000+ doctors, 76,000+ clinics.
- **Booking:** Instant — both in-person appointment and online consult; 60-second pro match for online consults.
- **Trust:** Stack of: gov ID + MCI reg verified + degree verified + Bluebook specialization match + only-verified-patient reviews + manual review moderation. Verified profiles get >95% of patient views.
- **Search facets:** Specialty × city × locality × language × consult type.
- **Pricing:** Per-consult, fixed by doctor.
- **Novel:** "Bluebook" — an internal mapping table that ties degrees to permitted specializations, preventing over-claiming. Direct analog for Safar: "you can list as Pandit only if vedic-credential field is populated."
- **Sources:** [Practo profile guidelines](https://help.practo.com/practo-profile/guidelines-for-creating-profiles/), [Steps to get listed](https://help.practo.com/practo-ray/getting-started/steps-to-get-listed-on-practo/), [Doctor profile](https://www.practo.com/providers/doctors/profile)

### Urban Company
- **Onboarding:** Treated explicitly as a "filtration funnel, not signup funnel." Steps: gov ID check → police verification → in-person interview → skill assessment → reference check → 3-45 day training (varies by category). 30,000+ trained partners.
- **Listing:** Service-as-listing — vendor is hidden behind UC catalog. Customer books a service, UC matches a pro.
- **Booking:** Instant — under 30 minutes for some categories.
- **Trust:** Stack: gov ID + police verification + skill assessment + on-arrival selfie matched against profile via facial recognition + insurance + SOS button + refund/re-service guarantee + Microsoft Azure Cognitive Services for safety.
- **Search facets:** Service × time slot × city (limited filtering — UC matches, doesn't show all pros).
- **Pricing:** Fixed catalog price set by UC.
- **Novel:** **On-arrival selfie facial-match** — at the customer's door, the partner takes a selfie which is matched to their profile photo to confirm the same person is showing up. Stops "send-a-cousin" fraud.
- **Sources:** [Partner app](https://partner.urbancompany.com/), [Trust playbook](https://yourstory.com/2026/02/urban-company-built-trust-indias-home-services-market), [Safety/Azure](https://www.urbancompany.com/blog/urban-company-strengthens-safety-protocol-using-microsoft-azure-cognitive-services), [Upskilling blog](https://www.urbancompany.com/blog/setting-up-service-partners-for-success-upskilling-at-urban-company)

### Sulekha
- **Onboarding:** Self-serve "List Your Business" → document verification → Free Trial leads → recharge to convert to PAID. 2000+ categories.
- **Listing:** Vendor-as-listing. Pay-per-lead model on most categories.
- **Booking:** No transaction — Sulekha forwards leads (form fills) to providers.
- **Trust:** Document verification + reviews.
- **Search facets:** 2000+ categories × city × user need (clustered around Home/Life/Self).
- **Pricing:** Vendor-set; Sulekha charges vendor per-lead.
- **Novel:** Free-trial-to-paid lead-economy model. Useful B2B revenue track for Safar if commission-on-booking proves friction-heavy.
- **Sources:** [List your business](https://www.sulekha.com/list-your-business), [FAQ](https://www.sulekha.com/local-services/business-owners/business/faq)

### JustDial
- **Onboarding:** Self-serve "List Free" with OTP verification of business name + address + contact + category. KYC unlocks better badges.
- **Listing:** Business-as-listing. Universal taxonomy.
- **Booking:** Phone-call routing primary (no in-platform transaction except via JD Mart B2B).
- **Trust:** "JD Trust" + "JD Verified" badges gated on **KYC + ≥3.8★ rating** (dual gate). "JDRR Certificate" available. "Biz Boosters" paid add-ons (rotational banners, badges, certificate, custom website).
- **Search facets:** Business name × city × category.
- **Pricing:** Vendor-stated; not transactional.
- **Novel:** Dual-gated trust badge (KYC alone doesn't qualify — must also have a 3.8★ history).
- **Sources:** [Badges](https://www.justdial.com/Badges), [Free Listing](https://www.justdial.com/Free-Listing), [Advertise](https://www.justdial.com/Advertise)

### Housejoy
- **Onboarding:** Self-serve at `/service-partner` → submits Gov ID + Local + Permanent Address Proof → interview + reference + license check + skill assessment.
- **Listing:** Service-as-listing with auto-allocation (customer doesn't pick a specific pro).
- **Booking:** Instant via app; auto-allocated.
- **Trust:** Document stack + assessment.
- **Search facets:** Category × city.
- **Pricing:** Fixed catalog set by Housejoy.
- **Novel:** Auto-allocation removes pro-selection from customer entirely.
- **Sources:** [Service partner](https://www.housejoy.in/service-partner), [FAQ](https://www.housejoy.in/faq-and-terms)

### The Knot (US)
- **Onboarding:** Self-serve via "WeddingPro" portal. Storefront listing across 25 categories.
- **Listing:** Vendor-as-listing; 3M+ reviews aggregated.
- **Booking:** Inquiry → vendor responds with quote.
- **Trust:** "Newlywed reviews" specifically aggregated; response rate; review volume.
- **Search facets:** 25 categories × city × style × **date availability** (filter to vendors with your date free).
- **Pricing:** Vendor-set tiers.
- **Novel:** Date-availability filter — most India platforms don't expose this even though wedding dates are the #1 user constraint.
- **Sources:** [Marketplace](https://www.theknot.com/marketplace), [WeddingPro](https://pros.weddingpro.com/), [Vendor home](https://www.theknot.com/vendors/home)

### Zola (US)
- **Onboarding:** Self-serve. Free listing + paid upgrades for placement.
- **Listing:** Vendor-as-listing storefront.
- **Booking:** "Pay-to-connect" — vendor only pays when a vetted lead is real and converts to conversation. Budget tracker integrates with vendor payments.
- **Trust:** Vetted-lead system, reviews, organic (not pay-to-rank) ranking.
- **Search facets:** Category × style × date-available × price.
- **Pricing:** Tiered packages; integrates into a budget tracker.
- **Novel:** **Pay-to-connect economics** — flips lead-economy from "vendor pays per impression" to "vendor pays per conversation." Better signal-to-noise.
- **Sources:** [For vendors](https://www.zola.com/for-vendors), [Marketplace plans](https://www.zola.com/inspire/vendors/how-plans-work), [Cost FAQ](https://www.zola.com/faq/360002891772-what-does-it-cost-to-be-listed-on-zola-)

### Etsy
- **Onboarding:** Self-serve shop wizard. Multi-session save-and-resume; progress bar showing % listings completed.
- **Listing:** Item-as-listing inside a shop container.
- **Booking:** Instant per-item cart.
- **Trust:** Star Seller program, response time, reviews.
- **Search facets:** Per-category dynamic attributes — choosing category determines which fields appear (material, color, occasion, holiday, dimensions). Attributes affect search ranking.
- **Pricing:** Per-item.
- **Novel:** **Category-driven dynamic attribute schema** — the listing form re-skins itself per category. Direct analog to Safar's table-per-type with type-specific fields.
- **Sources:** [Categories & attributes](https://www.etsy.com/seller-handbook/article/362857340643), [How to create a listing](https://help.etsy.com/hc/en-us/articles/115015628707), [Anatomy of a listing](https://www.etsy.com/seller-handbook/article/1347574487014)

### Fiverr
- **Onboarding:** Self-serve gig wizard.
- **Listing:** Gig-as-listing with mandatory Basic/Standard/Premium tiers.
- **Booking:** Instant — buyer purchases the gig package + extras at checkout.
- **Trust:** Seller level (New / Level 1 / Level 2 / Top Rated), response rate, completion rate.
- **Search facets:** Category × budget × delivery time × seller level.
- **Pricing:** **Standardized Gig Packages** — each category enforces mandatory fields (e.g. word count, revisions, delivery days) that all gigs in that category must populate. Forces apples-to-apples comparison. Extras are optional add-ons.
- **Novel:** Category-mandatory pricing fields = standardized comparison without losing seller flexibility. Strongest pattern for Safar's "different verticals, different schemas, but consistent compare UX."
- **Sources:** [Standardized packages](https://help.fiverr.com/hc/en-us/articles/4410009235601), [What are packages](https://help.fiverr.com/hc/en-us/articles/360010559138), [Glossary](https://help.fiverr.com/hc/en-us/articles/360010452397), [Categories](https://www.fiverr.com/categories)

---

## Patterns worth stealing (with rationale)

1. **Fiverr's category-mandatory fields → Safar's table-per-type with required columns.** [Goal C] Each provider type (Cake, Singer, Pandit, Decor, Staff, Appliance) declares its own required fields and pricing-unit. Customer's compare grid renders type-aware columns. **Implements:** the JOINED inheritance with type-table-owns-its-fields decision.

2. **WedMeGood's 5-channel parallel inquiry (Call/SMS/WhatsApp/Email/Chat) → Safar's booking-detail action bar.** [Goal B] On the booking detail page, render: "Open chat", "Call vendor", "WhatsApp vendor (pre-filled with booking ID)", "View ordered item". **Implements:** the booking↔catalog deep-link goal *and* India WhatsApp expectation.

3. **Practo's Bluebook (degree → permitted specializations) → Safar's verification-gates-listing-type.** [Goal A] A pandit applicant cannot publish in the Pandit table without a `vedic_credential` field; a cake baker cannot publish without `fssai_license_no`. Make the gate machine-enforceable in the publish workflow.

4. **Etsy's progress-bar + save-and-resume onboarding → Safar's vendor wizard.** [Goal A] Most India SMB vendors will start onboarding on a phone screen between calls. A multi-session draft state with a clear progress indicator dramatically improves completion rate.

5. **JustDial's dual-gate badge (KYC AND ≥3.8★) → Safar's tiered verification.** [Goal A] "Verified" should require KYC + first booking + minimum rating. "Top Rated" should require additional thresholds. Don't give all credibility on day-one approval.

6. **FNP's occasion-led IA → Safar's event-led service browse.** [Goal D] Top-level browse should let users pick "I'm planning: Birthday / Anniversary / Wedding / Housewarming / Pooja / Engagement" before they pick a service type. This drives bundling (cake + decor + singer for a birthday).

7. **VenueMonk's flexible-payment model → Safar's "pay-through-Safar OR pay-direct" toggle.** [Goal D] India vendors often resist gateway fees. Letting customers pay either path keeps friction low; Safar can still capture the booking for warranty/SLA purposes.

8. **Urban Company's on-arrival selfie facial-match → Safar's day-of OTP/selfie verification.** [Goal A/B] Already partially in Safar via day-of OTP; adding a vendor selfie at job-start would close "send-a-cousin" fraud, especially for staff-hire and pandit categories.

9. **Bakingo's instant slot picker (same-day / midnight / 60-min) → Safar's cake-vendor commitment SLA.** [Goal C] Cake category needs an availability/slot UI distinct from photographer (which is calendar-day) or pandit (which is muhurat-based).

10. **Zola's pay-to-connect → Safar's lead pricing if subscription becomes friction.** [Goal A] If commission-on-booking has high vendor pushback, pay-per-vetted-lead is a fallback monetization that aligns vendor incentives with platform.

---

## Patterns to avoid (anti-patterns observed)

1. **Pure inquiry-form-blast (ShaadiSaga's RFQ-to-many)** without instant-book fallback for catalog-friendly categories (cake, appliance, staff). Customers wanting a 1-kg chocolate cake do not want to wait for 5 quotes. **Apply:** instant-book for catalog types, RFQ for bespoke types — never one model for both.

2. **JustDial's phone-only routing.** Sending the customer off-platform to call kills the booking deep-link, eliminates rating capture, and breaks SLA enforcement. **Apply:** all comms must thread through a Safar booking-id-bearing chat or WhatsApp template.

3. **Hidden vendors (Urban Company / Housejoy auto-allocation).** Wedding/event services are personality-driven — customers want to *pick* the singer or pandit. Auto-allocation may suit appliance rental or basic staff but is wrong for emotional-purchase categories.

4. **Subscription-as-only-monetization (WedMeGood).** Many small Indian vendors won't pay a flat ₹X/month upfront. **Apply:** offer commission-on-booking as the default, with subscription as an upgrade for high-volume vendors who want unlimited leads.

5. **Vague "verified" badges with no underlying signal (some Sulekha/JustDial free listings).** A "verified" tooltip should always reveal *what* was verified (PAN? FSSAI? Police? GST?). Surface the underlying credential, not just the badge.

6. **Generic listing form across all categories (early Sulekha).** Forces cake bakers and pandits into the same fields, producing low-quality data and weak filtering. **Apply:** Etsy/Fiverr-style category-driven dynamic schema.

---

## India-specific insights

1. **WhatsApp is non-negotiable.** Every successful India platform (WedMeGood, ShaadiSaga, JustDial, Sulekha) treats WhatsApp as a primary support and booking-comms channel. A "WhatsApp this vendor" button on the booking detail (with pre-filled booking ID context) is not optional.

2. **Trust is layered, not single-shot.** Indian users distrust a single "Verified" badge. They want to see: years-in-business + reviews count + KYC + license number + photos + previous-customer reference. Surface multiple signals.

3. **Licenses are vertical-specific and legally required.**
   - Cake/cook: **FSSAI** Basic (≤₹1.5 Cr turnover) / State (₹1.5 Cr–₹50 Cr) / Central (>₹50 Cr or e-commerce). Operating without FSSAI on any e-commerce platform is illegal and carries 6-month imprisonment + ₹5L fine.
   - Pandit: no statutory license; rely on community references + temple affiliation.
   - Singer/Decorator: GST if turnover >₹20L; otherwise none.
   - Staff-hire: police verification + ID proof.
   - **Implication:** Safar's verification table needs per-type fields, not one-size-fits-all.

4. **Tier-2/3 vendor tech literacy is low.** WedMeGood and ShaadiSaga both ship dedicated mobile apps for vendors — vendors are more comfortable on Android than on a desktop dashboard. Onboarding wizard must be mobile-first.

5. **Quote-on-request is culturally accepted for events.** Unlike US (The Knot moving toward instant-book), India weddings are genuinely bespoke and a quote model fits. Don't force instant-book on decor/pandit/singer; do offer it for cake/appliance/staff/cake-cutting-cake.

6. **Date-availability is the killer filter.** India weddings cluster around muhurat dates (often <100 auspicious dates/year). The Knot does this well; no India platform researched does it cleanly. Easy win.

7. **Subscription resistance.** Sulekha's free-trial-then-recharge model exists because Indian SMBs resist upfront commitments. Lead-economy or commission-on-booking lowers friction.

8. **Multi-vendor RFQ broadcast (ShaadiSaga) is a culturally-fit pattern** — Indian customers expect to negotiate across 3–5 quotes for high-value events. Building a "send to 5 matching vendors" CTA is a strong fit, but only for quote-driven types.

---

## Open questions

1. **Exact onboarding step count** for WedMeGood / ShaadiSaga / Sulekha — public marketing pages don't expose the wizard step list; would need to actually complete a vendor signup to confirm. Search results suggest 4–6 steps but unverified.
2. **Booking row → service-item deep-link existence** on competitors. None of the searched platforms publicly document this. Anecdotal evidence (review forums) suggests the booking row links back to the *vendor profile* but rarely to the *specific item ordered*. Safar's Goal B may be a genuine differentiator.
3. **Whether WedMeGood enforces FSSAI / GST upload** at vendor onboarding for food categories — ToS says vendors must hold "necessary licenses" but doesn't say they're collected/verified. Likely a gap to exploit.
4. **Practo's Bluebook implementation** — is it a static rule table or ML-driven? Could not determine from public docs.
5. **Urban Company's URL structure for service items** — could not extract from search snippets; would need direct page fetch.
6. **Acceptance rate / approval SLA** at WedMeGood and ShaadiSaga — claimed "vetted" but no published SLA.
7. **Hybrid catalog-driven vs RFQ-driven UX in one platform** — none of the researched platforms cleanly hybridize. Safar implementing both per-vertical may be a novel design space.

---

**End of teardown. ~2,400 words.**
