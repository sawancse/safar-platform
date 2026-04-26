# Safar Activation Playbook

A single doc you can follow item-by-item to flip every external-blocker into "live."
Each section: **what to do**, **where**, **expected response**, and the **env vars to set after**.

When you finish each item, replace the ⏳ with ✅ and date.

---

## 🔴 Tier 1 — Blocking go-live (do these first)

### 1. Send TBO Air partner application email ⏳
- **What:** Apply for B2B partnership + sandbox API credentials
- **Where:** Email already drafted at `docs/flight-aggregator-partner-outreach.md` (Email 3 — TBO Air)
- **To:** `apisupport@tboair.com` + form at `https://apiintegration.tboholidays.com`
- **Expected response:** 1–4 weeks. Will get sandbox creds + API docs after KYC + deposit/bank-guarantee terms confirmed
- **Env vars to set after:**
  ```bash
  TBO_ENABLED=true
  TBO_USERNAME=<your-username>
  TBO_PASSWORD=<your-password>
  TBO_AGENCY_ID=<your-agency-id>
  TBO_BASE_URL=https://Affiliate.tektravels.com  # default; sandbox URL may differ
  ```
- **Engineering follow-up:** fill the 5 `// TODO(tbo-creds)` blocks in `services/flight-service/src/main/java/com/safar/flight/adapter/tbo/TBOAirFlightAdapter.java` (~3-4 hrs)

### 2. Send TripJack partner application email ⏳
- **What:** Apply for B2B partnership (Indian LCC primary candidate)
- **Where:** Email drafted at `docs/flight-aggregator-partner-outreach.md` (Email 2 — TripJack)
- **To:** `partners@tripjack.com` + form at `https://tripjack.com/nav/b2b-portal-for-travel-agents`
- **Expected response:** 3-7 days
- **Env vars to set after:** TBD (TripJack adapter not yet built — when their docs arrive, build adapter following TBO pattern, ~6 hrs)

### 3. Send TravClan partner application email ⏳
- **What:** Apply (fastest-response, most flexible terms)
- **Where:** Email drafted at `docs/flight-aggregator-partner-outreach.md` (Email 1 — TravClan)
- **To:** `hello@travclan.com` + Chirag Agrawal LinkedIn DM
- **Expected response:** 1-3 days
- **Env vars to set after:** TBD (adapter not yet built)

---

## 🟡 Tier 2 — Activates dormant code (do once Tier 1 partner is live)

### 4. MSG91 DLT registration — 4 SMS templates ⏳
- **What:** Register 4 SMS templates with TRAI/MSG91 for transactional sends
- **Where:** MSG91 dashboard → SMS → DLT templates
- **Templates needed:**
  | Template name | Variables |
  |---|---|
  | flight-confirmed | `{ref}, {route}, {date}, {flight}` |
  | flight-cancelled | `{ref}, {route}, {refund}` |
  | flight-checkin | `{ref}, {route}, {flight}` |
  | flight-search-abandoned | `{route}, {date}, {fare}` |
- **Expected response:** 1-2 weeks (TRAI approval cycle)
- **Env vars to set after:**
  ```bash
  MSG91_AUTH_KEY=<your-msg91-auth-key>
  MSG91_SENDER_ID=SAFAR
  MSG91_SMS_FLIGHT_CONFIRMED_TEMPLATE_ID=<id>
  MSG91_SMS_FLIGHT_CANCELLED_TEMPLATE_ID=<id>
  MSG91_SMS_FLIGHT_CHECKIN_TEMPLATE_ID=<id>
  MSG91_SMS_FLIGHT_SEARCH_ABANDONED_TEMPLATE_ID=<id>
  ```

### 5. MSG91 WhatsApp — 5 templates Meta-approved ⏳
- **What:** Submit 5 WhatsApp Business templates via MSG91 → Meta Business Manager
- **Where:** MSG91 dashboard → WhatsApp → Templates → Submit to Meta
- **Templates needed:**
  | Template name | Body |
  |---|---|
  | flight-search-abandoned | "Still going {{1}} on {{2}}? {{3}} Tap to book." |
  | flight-booking-confirmed | "Your flight {{1}} for {{2}} on {{3}} ({{4}}) is confirmed. Carry valid ID." |
  | insurance-policy-issued | "Insurance {{1}} issued via {{2}}. Coverage: {{3}}. Cert: {{4}}" |
  | book-flight-deeplink | "Tap to book your flight on Safar: {{1}}" |
  | book-insurance-deeplink | "Tap to book travel insurance on Safar: {{1}}" |
  | bot-help | "Reply BOOK FLIGHT, BOOK INSURANCE, STATUS, or HELP" |
- **Expected response:** 1-3 weeks per template (Meta approval cycle)
- **Env vars to set after:**
  ```bash
  MSG91_WA_INTEGRATED_NUMBER=<your-meta-verified-WA-number>
  MSG91_WA_NAMESPACE=<your-meta-namespace>
  MSG91_WA_FLIGHT_SEARCH_ABANDONED_TEMPLATE=flight_search_abandoned
  MSG91_WA_FLIGHT_BOOKING_CONFIRMED_TEMPLATE=flight_booking_confirmed
  MSG91_WA_INSURANCE_POLICY_ISSUED_TEMPLATE=insurance_policy_issued
  MSG91_WA_BOOK_FLIGHT_DEEPLINK_TEMPLATE=book_flight_deeplink
  MSG91_WA_BOOK_INSURANCE_DEEPLINK_TEMPLATE=book_insurance_deeplink
  MSG91_WA_BOT_HELP_TEMPLATE=bot_help
  MSG91_WA_WEBHOOK_SECRET=<your-webhook-secret>  # for inbound HMAC verification
  ```
- **Also configure:** MSG91 dashboard → WhatsApp → Webhooks → POST URL: `https://api.ysafar.com/api/v1/whatsapp/webhook`

### 6. Acko Insurance partnership ⏳
- **What:** Apply for B2B partnership for travel insurance
- **Where:** `https://acko.com/business` → product partnerships → travel insurance
- **Expected response:** 1-2 weeks
- **Env vars to set after:**
  ```bash
  ACKO_ENABLED=true
  ACKO_API_KEY=<your-api-key>
  ACKO_PARTNER_ID=<your-partner-id>
  ACKO_BASE_URL=https://api.acko.com  # default
  ```
- **Engineering follow-up:** fill 3 `// TODO(acko-creds)` blocks in `services/insurance-service/src/main/java/com/safar/insurance/adapter/acko/AckoInsuranceAdapter.java` (~3 hrs)

### 7. ICICI Lombard Insurance partnership ⏳ (optional Tier-2 backup)
- **What:** Apply for B2B partnership (more enterprise; backup to Acko)
- **Where:** ICICI Lombard B2B partner team contact
- **Expected response:** 2-4 weeks
- **Env vars to set after:**
  ```bash
  ICICI_LOMBARD_ENABLED=true
  ICICI_LOMBARD_API_KEY=<your-api-key>
  ICICI_LOMBARD_PARTNER_CODE=<your-partner-code>
  ICICI_LOMBARD_BASE_URL=https://api.icicilombard.com  # default
  ```
- **Engineering follow-up:** fill 3 `// TODO(icici-creds)` blocks in `services/insurance-service/src/main/java/com/safar/insurance/adapter/icici/ICICILombardInsuranceAdapter.java` (~3 hrs)

---

## 🟢 Tier 3 — Engineering tasks not yet done (no external blocker)

### 8. Mobile Expo push token capture ⏳
- **What:** Mobile app captures Expo token on first launch + POSTs to user-service
- **Where:** `frontend/mobile/` (React Native + Expo SDK 51)
- **Code to write:**
  - `expo-notifications` registerForPushNotificationsAsync() on app boot
  - POST to `/api/v1/users/me/push-tokens` with `{pushToken, platform, deviceId}` (endpoint already live in user-service)
- **Effort:** ~2 hrs
- **Activates:** push channel for abandoned-search reminders + future push notifications

### 9. TBO sandbox testing once creds arrive ⏳
- **What:** End-to-end test of the full lifecycle in TBO sandbox
- **Steps:** search → fareQuote → book → cancel/refund → verify webhook + Kafka events fire
- **Effort:** ~30 min once token is set
- **Why:** Confirms the adapter actually works against real TBO before promoting to prod

---

## ✅ Already done — code paths LIVE, just waiting on the above

| Item | Status |
|---|---|
| TBO adapter framework + scaffold | ✅ Built (waiting on TBO sandbox creds) |
| Acko + ICICI Lombard adapter scaffolds | ✅ Built (waiting on partnerships) |
| Universal Trip schema + service + auto-creation from flight | ✅ Live |
| Trip Intent rule engine (40 seed rules) | ✅ Live |
| "Complete your trip" hub UI on safar-web | ✅ Live (graceful no-op without backend) |
| Cancel propagation TripService → flight/insurance services | ✅ Live |
| Two-step admin refund approval queue | ✅ Live (auto < ₹10k, admin > ₹10k) |
| Daily Razorpay ↔ provider reconciliation job | ✅ Live (provider-agnostic framework) |
| Abandoned-search recovery: capture + detector + email + WA + push + SMS | ✅ Live (channels activate on env-var flip) |
| user-service `/flags` + `/push-tokens` endpoints | ✅ Live |
| WhatsApp Service: 5 outbound templates + inbound webhook bot | ✅ Live (templates activate on env-var flip) |

---

## Decisions parked (revisit later)

- **Duffel** — demoted to international-only earlier today; may reactivate when international flight volume justifies. Adapter is built and dormant.
- **Push token mobile-side capture** — out of session scope; needs mobile dev cycle
- **Multi-country broader platform readiness sprint** — needs strategic decision on first expansion country (UAE / SG / SE Asia)
- **WhatsApp Flows** for in-WA multi-step booking (rather than deep-link redirect) — Phase 2

---

## Branch / deployment state at end of session

- **safar-platform**: `feat/scm-vendors-adapters` — all today's flight + insurance + WA work pushed
- **safar-web**: `master` — `CompleteYourTrip` hub component pushed; Amplify auto-deploys on master push (component gracefully no-ops if backend not yet deployed)
- **safar-platform deploy**: ECS deployment is manual; backend (gateway + booking + flight + insurance + notification) needs redeploy for Trip API + WA webhook + insurance routes to be live in prod
