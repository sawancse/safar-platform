# BhramanKaro — Google Play Store Listing Copy

Ready-to-paste content for the Play Console "Main store listing" + supporting fields.
Character limits noted per field; current copy is within limits.

---

## App name  (max 30 chars)
```
BhramanKaro: Stays & Travel
```
*(27 chars. Alternatives: "BhramanKaro – Travel India" / "BhramanKaro: Book & Travel")*

## Short description  (max 80 chars)
```
Book stays, PGs, flights, home chefs & event services across India.
```
*(67 chars.)*

## Full description  (max 4000 chars)
```
BhramanKaro is India's all-in-one travel and living marketplace. Whether you're booking a weekend getaway, finding a long-term PG, hiring a home chef, or planning an event — it's all in one app.

🏡 STAYS FOR EVERY TRIP
Browse and book homes, hotels, resorts, heritage havelis, villas and unique stays across India. Filter by price, rating, instant book and property type. See verified listings, real photos, and honest reviews before you book.

🛏️ PG, CO-LIVING & MONTHLY RENTALS
Find verified PGs, co-living spaces and hostels with transparent monthly pricing, sharing options, deposits and amenities. Manage your tenancy, rent, agreements and maintenance requests right from the app.

✈️ FLIGHTS
Search and book domestic and international flights at great fares, manage your trips, and get check-in reminders.

👨‍🍳 HOME CHEFS & COOKS
Hire professional home chefs and daily cooks for everyday meals, parties and special occasions. Pick dishes from a curated catalog, set your menu, and track your booking end to end.

🎉 EVENT & HOME SERVICES
Book trusted vendors for cakes, decoration, pandits, singers, party staff and appliance rentals — with verified profiles, ratings and clear pricing.

🛡️ INSURANCE & MORE
Compare and buy travel, health, motor and term insurance plans from leading insurers, and explore home loans — without leaving the app.

🏷️ BUY, SELL & PROJECTS
Discover properties for sale, new builder projects, and list your own property.

WHY BHRAMANKARO
• Verified listings and KYC-checked hosts and providers
• Secure payments with instant confirmations
• Transparent pricing — no hidden charges
• Real reviews from real guests
• Fast support and easy cancellations
• Built for India, in English and Hindi

Download BhramanKaro and travel, stay and celebrate — the smarter way.

By using BhramanKaro you agree to our Terms of Service and Privacy Notice available at bhramankaro.com.
```

---

## Supporting fields

| Field | Value |
|-------|-------|
| **App category** | Travel & Local |
| **Tags** (choose up to 5) | Hotel & travel booking, Real estate, Local services, Transportation, Lifestyle |
| **Contact email** | support@bhramankaro.com |
| **Contact website** | https://bhramankaro.com |
| **Privacy policy URL** | https://bhramankaro.com/privacy |
| **Default language** | English (United States) — en-US (add Hindi hi-IN as a second listing later) |

---

## Required graphic assets (you must supply these before publishing)

| Asset | Spec | Notes |
|-------|------|-------|
| **App icon** | 512 × 512 px, 32-bit PNG | Already have `assets/icon.png` — export at 512² |
| **Feature graphic** | 1024 × 500 px, PNG/JPG | Banner shown at top of listing — **mandatory** |
| **Phone screenshots** | 2–8 images, 16:9 or 9:16, min 320px side | Capture: Home/search, listing detail, booking, cooks, profile |
| **(Optional) Tablet screenshots** | 7" and 10" | Improves listing on tablets |
| **(Optional) Promo video** | YouTube URL | — |

> Tip: capture screenshots from the installed preview APK on a phone, or an emulator.

---

## Content rating questionnaire (answers for IARC)
- App contains no violence, sexual, or drug content → expected rating **3+ / Everyone**.
- Does it let users interact / share location? **Yes** (messaging between guests/hosts, location used for search). Declare accordingly.
- Digital purchases: **Yes** (bookings/payments via Razorpay — physical services, not IAP).

---

## Data Safety form mapping (declare in Play Console → App content → Data safety)

Collected & linked to the user:
- **Personal info**: Name, Email, Phone number, Address, Government ID (KYC) — purposes: App functionality, Account management, Fraud prevention.
- **Financial info**: Payment info — handled by Razorpay (PCI-DSS); purpose: App functionality.
- **Location**: Approximate + Precise (optional, consent-gated) — purpose: App functionality.
- **Photos**: user-uploaded — purpose: App functionality.
- **App activity & device IDs**: usage, push token — purposes: Analytics, App functionality.

Security practices to declare:
- Data encrypted in transit (HTTPS): **Yes**
- Users can request data deletion: **Yes** → link the deletion path (Profile → Settings → Delete Account, or email privacy@bhramankaro.com)
- Data NOT sold to third parties.

---

## Notes / open items
- Confirm `support@bhramankaro.com` and `privacy@bhramankaro.com` mailboxes are live and monitored.
- New **personal** Play accounts require 20 testers × 14 days closed testing before production; an **organization** account avoids this.
- First `.aab` upload must be done manually in Play Console; later releases can use `eas submit`.
- Production `.aab` ready: build d15cf8a1 (versionCode 2) — see EAS build list.
