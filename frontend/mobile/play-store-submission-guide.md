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
   - **App access** → "All functionality is available without special access" **unless** reviewers
     need a login. Safar requires OTP login to see most flows → choose *"All or some functionality
     is restricted"* and provide a **test login**: a phone number + the dev OTP. ⚠️ Dev OTP is
     `123456` (hardcoded in auth-service dev mode) — only works if the backend the app points at
     runs in dev OTP mode. Confirm the tunnel-served backend accepts `123456` for the reviewer's
     number, or create a real reviewer account with a known OTP path. **Do not ship a build that
     reviewers can't log into.**
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
| Does the app share the user's current physical location with other users? | **Yes** — location is used for search and can be shared in host/guest messaging. |
| Does the app allow users to interact or exchange content / communicate? | **Yes** — guest↔host and customer↔provider messaging. |
| Does the app allow purchase of digital goods? | **No** — purchases are physical/real-world services (stays, flights, chef, insurance), processed by Razorpay. Not digital IAP. |
| Does the app contain user-generated content shared with others? | **Yes** — reviews, photos, messages. |
| Miscellaneous (data collection/sharing prompts) | Answer truthfully per §3; no NSFW/illegal content. |

**Expected result:** PEGI 3 / ESRB Everyone / IARC 3+ (the "communicates / shares location"
flags add an interaction notice but do not raise the age rating). Re-run the questionnaire if any
feature materially changes.

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
| **Location → Approximate location** | Yes | Optional | App functionality (search) |
| **Location → Precise location** | Yes | Optional | App functionality (nearby search / map pin) — consent-gated |
| **Photos and videos → Photos** | Yes | Optional | App functionality (listing/review/KYC uploads) |
| **Messages → Other in-app messages** | Yes | Optional | App functionality (guest↔host / customer↔provider chat) |
| **App activity → App interactions** | Yes | Optional | Analytics; App functionality |
| **App info & performance → Crash logs / Diagnostics** | Yes | Optional | Analytics; App functionality |
| **Device or other IDs → Device or other IDs** | Yes | Optional | App functionality (push notifications / FCM token) |

**Data shared with third parties:** declare **None** for "shared" in the sharing sense. Razorpay
(payments), FCM (push), and any analytics SDK are **service providers / processors** — Play's Data
Safety treats processor relationships as *collection*, not *sharing*, **as long as** they only
process on your behalf. Keep "Shared = No" unless an SDK uses data for its own purposes.

> ⚠️ Audit before final submit: if the app bundles Google Analytics/Firebase Analytics or any ad
> SDK, confirm what each collects and whether it qualifies as "sharing." Update this table to match
> the SDKs actually compiled into versionCode 2. Mis-declaring Data Safety is a common rejection cause.

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
- [ ] **Reviewer login** — ensure Google's reviewer can actually log in (dev OTP `123456` only works
      if the pointed-at backend is in dev mode; otherwise provision a real reviewer number + OTP).
- [ ] **ToS jurisdiction** — `safar-web/app/terms` says Hyderabad, Telangana; confirm it matches the
      registered office of BhramanKaro India Pvt. Ltd.
- [ ] **support@ / privacy@ mailboxes** live and monitored.
- [ ] **Bundled SDK audit** for the Data Safety "sharing" question (analytics/ads/Firebase).
- [ ] **Feature graphic** (1024×500) produced — mandatory blocker.
```
