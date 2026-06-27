# BhramanKaro — Google Play Submission Guide

Step-by-step companion to `play-store-listing.md`. This file holds the **exact answers**
for the two questionnaires Play Console makes you click through (Data Safety + Content
Rating), the first-upload sequence, and how to switch on `eas submit` for later releases.

Current build to upload: **production .aab, versionCode 2** (EAS build `d15cf8a1`).
Artifact: `https://expo.dev/artifacts/eas/xyAsGuaXNybOCRRKzFWyxztmvgzn2f5vRAPv0eYzxk4.aab`

---

## 0. Pre-flight (do once, before opening Play Console)

- [ ] **Personal vs Organisation account decision** — see `play-store-listing.md`. Org skips the
      20-tester × 14-day closed-test gate; personal does not.
- [ ] Confirm `support@bhramankaro.com` **and** `privacy@bhramankaro.com` mailboxes are live and monitored
      (Data Safety + listing reference both; Google may email the contact address to verify).
- [ ] Privacy policy live: `https://bhramankaro.com/privacy` ✅ (pushed `04258bf`).
- [ ] Account-deletion path reachable & described in the privacy policy ✅ (required for Data Safety).
- [ ] Graphic assets ready (see `play-store-listing.md` → Required graphic assets): 512² icon,
      1024×500 feature graphic, 2–8 phone screenshots. **Feature graphic is mandatory — listing
      cannot be submitted without it.**

---

## 1. First-release upload sequence (manual — API cannot create the first release)

1. **Create app** in Play Console → *Create app*.
   - Name: `BhramanKaro: Stays & Travel` · Default language: `English (US)` · App · Free.
   - Tick the two declarations (Developer Program Policies + US export laws).
2. **Set up your app** dashboard — work top to bottom:
   - **App access** → BhramanKaro requires OTP login to see most flows → choose *"All or some
     functionality is restricted"* and add an instruction with the verified test login below.
     See **§1a Reviewer login** for the exact text to paste. ✅ Verified working end-to-end
     against the live tunnel on 2026-06-27 (account pre-created, `123456` accepted).
   - **Ads** → Declare whether the app contains ads. BhramanKaro shows **no ads** → *No*.
   - **Content rating** → fill the IARC questionnaire → use §2 answers below.
   - **Target audience** → 18+ (financial transactions, rentals). Not designed for children.
   - **Data safety** → use §3 answers below.
   - **Government apps** → No.
   - **Financial features** → Declare: the app facilitates payments for bookings/insurance/loans
     via Razorpay. It is **not** a lending app itself (loans are referrals/marketplace). Tick
     "None of the above" for the regulated-lending categories unless you directly disburse credit.
     Insurance is sold as a marketplace/aggregator — declare if prompted.
3. **Store listing** → paste from `play-store-listing.md` (name, short, full desc), add graphics.
4. **Production** (or **Closed testing** first if personal account) → *Create release*.
   - **Upload the .aab** (versionCode 2 artifact above). App signing by Google Play → **opt in**
     (let Google manage the signing key; EAS upload key stays as the upload cert).
   - Release name auto = `2 (1.0.0)`. Add release notes (see §4).
5. **Review & roll out.** First review typically 1–7 days.

> Personal-account path: do steps as **Closed testing** track, add ≥20 testers (email list or
> Google Group), keep the test running **14 continuous days**, then *Promote release* → Production.

---

## 1a. Reviewer login — Play Console "App access" (VERIFIED 2026-06-27)

In Play Console → *App content → App access* → choose **"All or some functionality is
restricted"** → *Add new instructions*:

| Field | Value |
|---|---|
| **Name** | Phone OTP login |
| **Username** (phone) | `6000012345` |
| **Password / any other** | OTP: `123456` |

**Instructions to paste:**
```
This app uses phone + OTP login.
1. Open the app and tap Profile (bottom tab) → Login.
2. Keep "Phone" selected. Enter mobile number: 6000012345
   (the app automatically adds the +91 India country code).
3. Tap Continue / Send OTP.
4. Enter the OTP: 123456
5. Tap Verify. You are now logged in as a test guest account.

Note: 123456 is the valid verification code for this test account.
A test account has already been created for this number.
```

**Verified facts (do not change without re-testing):**
- Account already exists: phone `+916000012345`, name "Play Reviewer", role GUEST. Re-login needs
  no name — just phone + `123456`.
- `otp.dev-mode: true` in `application.yml` **and** `application-prod.yml`; no `OTP_DEV_MODE`
  override → `123456` is accepted for any number regardless of profile.
- Tested live through `api.bhramankaro.com`: `otp/send` → 200, `otp/verify` with `123456` →
  returns access + refresh tokens.
- Rate limit is **3 OTP *sends* per phone per hour** (the verify step is not limited). Reviewers
  need only one send, so this won't bite — but don't spam the Send button while testing.

> ⚠️ **Backend dependency (because AWS is paused):** the app talks to the laptop backend via the
> `bhraman-local` Cloudflare tunnel. The tunnel + the 16-service stack **must be running** for the
> entire review window, or the reviewer sees blank data / login failures → rejection. Keep the
> laptop on and the `bhraman-local-tunnel` ScheduledTask running until the app is approved.

> 🔒 **Security note (not a blocker, fix later):** `dev-mode: true` in prod means *anyone* can log
> into *any* account on production with `123456`. Fine for review, but before real public launch set
> `OTP_DEV_MODE=false` and wire a real OTP provider (MSG91 is already coded — `OTP_PROVIDER=msg91`).
> If you flip it off, this reviewer account will need a real OTP path instead.

---

## 2. Content rating (IARC questionnaire) — exact answers

Category to choose first: **Utility, Productivity, Communication, or Other** (BhramanKaro is a
marketplace/utility, not a game). Then:

| Question | Answer |
|---|---|
| Violence (cartoon/fantasy/realistic) | **No** |
| Sexual content / nudity | **No** |
| Profanity or crude humour | **No** |
| Controlled substances (drugs/alcohol/tobacco) | **No** |
| Gambling (simulated or real-money) | **No** |
| Does the app share the user's current physical location with other users? | **No** — the app does not access device location (no location SDK); search uses typed city/text. |
| Does the app allow users to interact or exchange content / communicate? | **Yes** — guest↔host and customer↔provider messaging. |
| Does the app allow purchase of digital goods? | **No** — purchases are physical/real-world services (stays, flights, chef, insurance), processed by Razorpay. Not digital IAP. |
| Does the app contain user-generated content shared with others? | **Yes** — reviews, photos, messages. |
| Miscellaneous (data collection/sharing prompts) | Answer truthfully per §3; no NSFW/illegal content. |

**Expected result:** PEGI 3 / ESRB Everyone / IARC 3+ (the "users communicate" flag adds an
interaction notice but does not raise the age rating). Re-run the questionnaire if any feature
materially changes.

---

## 3. Data Safety form — exact per-type answers

For **every** data type below: *Collected* = Yes, *Shared* = No (we do not sell or share with
third parties for their own use; payment processing by Razorpay is a processor, declared as
processing not sharing), *Processed ephemerally* = No, unless noted. Mark *Required* vs *Optional*
as listed. Encryption in transit = **Yes** for all. Each type needs ≥1 purpose.

### Data collection & security (top-level)
- Does your app collect or share any of the required user data types? → **Yes**
- Is all of the user data collected by your app encrypted in transit? → **Yes** (HTTPS/TLS)
- Do you provide a way for users to request that their data be deleted? → **Yes**
  → URL: account deletion path (in-app Profile → Settings → Delete Account) + `privacy@bhramankaro.com`.

### Per-type declarations

| Data type (Play category → item) | Collected | Required? | Purposes |
|---|---|---|---|
| **Personal info → Name** | Yes | Required | App functionality; Account management |
| **Personal info → Email address** | Yes | Required | App functionality; Account management; Customer support |
| **Personal info → Phone number** | Yes | Required | App functionality (OTP login); Account management |
| **Personal info → Address** | Yes | Optional | App functionality (stay/PG/service delivery) |
| **Personal info → User IDs** | Yes | Required | App functionality; Account management |
| **Personal info → Other (Govt ID / KYC)** | Yes | Optional | App functionality; Fraud prevention, security & compliance |
| **Financial info → Purchase history** | Yes | Required | App functionality |
| **Financial info → Payment info** | Yes (via Razorpay) | Required | App functionality. *Note: card/UPI details captured by Razorpay (PCI-DSS), not stored by us.* |
| **Location → Approximate location** | **No** | — | App does **not** read device location (no `expo-location`, no maps SDK). Search uses typed city/text. *See SDK-audit note below.* |
| **Location → Precise location** | **No** | — | Same — not collected. |
| **Photos and videos → Photos** | Yes | Optional | App functionality (listing/review/KYC uploads via `expo-image-picker`) |
| **Messages → Other in-app messages** | Yes | Optional | App functionality (guest↔host / customer↔provider chat) |
| **App activity → App interactions** | **No** | — | No analytics SDK in the app (GA4 is on the **web** only). Backend access logs are operational, not product analytics. |
| **App info & performance → Crash logs / Diagnostics** | **No** | — | No Sentry/Crashlytics/Firebase Analytics bundled. (Play's own Android Vitals is not declarable here.) |
| **Device or other IDs → Device or other IDs** | Yes | Optional | App functionality (push notifications / FCM token via `expo-notifications`) |

**Data shared with third parties:** declare **None** for "shared" in the sharing sense. Razorpay
(payments) and FCM (push) are **service providers / processors** — Play's Data Safety treats
processor relationships as *collection*, not *sharing*. Keep "Shared = No".

### SDK audit results (done 2026-06-27 against versionCode 2 deps)

Audited `frontend/mobile/package.json` + `app.json` + code. SDKs that touch user data:
- `expo-notifications` (+ FCM) → push token → **Device IDs** (processor: Google/FCM, Shared:No)
- `expo-image-picker` (+ CAMERA/READ_MEDIA_IMAGES) → **Photos** (user-initiated)
- `expo-auth-session`/`expo-web-browser` → Google OAuth → Name+Email (already declared)
- `react-native-webview` → hosts Razorpay checkout (payment info handled by Razorpay)
- `expo-secure-store`/`async-storage`/`zustand` → on-device only, **not collected**

**Confirmed ABSENT** (declare "No" for all): Firebase/Google Analytics, AdMob/Facebook/any ad SDK,
Sentry/Crashlytics, Amplitude/Mixpanel/Segment, `react-native-maps`, `expo-location`.

> ⚠️ **ACTION REQUIRED — unused location permission.** `app.json` declares `ACCESS_FINE_LOCATION` +
> `ACCESS_COARSE_LOCATION` (and iOS `NSLocation*` strings) but **no code uses location** and there is
> no location SDK. Declaring an unused sensitive permission risks rejection under Google's Permissions
> policy. **Remove these from `app.json` before the Play build** (Android `permissions` array + iOS
> `infoPlist` location keys). Then location is cleanly "Not collected" as in the table above. This
> needs a rebuild + versionCode bump (required for a new Play release anyway). `WRITE_EXTERNAL_STORAGE`
> can also be dropped (not needed for image-picking on modern Android).

---

## 4. Release notes (en-US) — paste into the release

```
First release of BhramanKaro.
• Book stays, hotels, PGs and monthly rentals across India
• Search and book domestic & international flights
• Hire home chefs and cooks; book event services (cake, decor, pandit, singer, staff)
• Compare and buy insurance; explore home loans
• Secure payments, verified listings, real reviews
```

---

## 5. Activating `eas submit` for FUTURE releases (after the manual first upload)

`eas.json` is already wired:
```json
"submit": { "production": { "android": {
  "serviceAccountKeyPath": "./google-service-account.json",
  "track": "internal", "releaseStatus": "draft"
}}}
```
Steps to make it work:
1. Play Console → **Setup → API access** → link a Google Cloud project.
2. In Google Cloud → **IAM & Admin → Service Accounts** → create a service account → create a
   **JSON key** → save it as `frontend/mobile/google-service-account.json`
   (already gitignored ✅ — never commit it).
3. Back in Play Console → grant that service account the **Release apps to testing tracks** +
   **Manage production releases** permissions (or "Release manager").
4. Then: `eas submit --profile production --platform android` uploads the latest build to the
   **internal** track as a **draft**. Promote in Play Console, or change `track`/`releaseStatus`.

> The service account **cannot create the very first release** — that is why §1 must be done by hand.
> Once one release exists, `eas submit` handles every subsequent upload.

---

## 6. Open items / confirmations needed

- [ ] **Account type** (personal vs org) — gates whether closed testing is mandatory.
- [x] **Reviewer login** — ✅ DONE & VERIFIED 2026-06-27. Test account `6000012345` / OTP `123456`
      created and confirmed working live. See §1a. (Caveat: backend + tunnel must stay UP during review.)
- [x] **Feature graphic (1024×500)** — ✅ DONE. `frontend/mobile/assets/play-feature-graphic.png`
      (committed `d531d42`).
- [ ] **ToS jurisdiction** — `safar-web/app/terms` says Hyderabad, Telangana; confirm it matches the
      registered office of BhramanKaro India Pvt. Ltd.
- [ ] **support@ / privacy@ mailboxes** live and monitored.
- [x] **Bundled SDK audit** — ✅ DONE 2026-06-27 (see §3). No analytics/ads/crash SDK; location not
      collected. Data Safety table corrected.
- [x] **Remove unused location permissions** — ✅ DONE 2026-06-27. Dropped `ACCESS_FINE/COARSE_LOCATION`
      + `WRITE_EXTERNAL_STORAGE` + iOS `NSLocation*` from `app.json`. versionCode bump is automatic
      (eas.json `appVersionSource: remote` + `autoIncrement` → next prod build = vCode 3).
      **Remaining step: rebuild** `eas build --profile production --platform android` so the AAB drops
      the location permission, then upload that build.
- [x] **Feature graphic** (1024×500) — produced (see above).
```
