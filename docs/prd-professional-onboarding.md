# PRD: Professional Onboarding — Marketplace Self-Registration

**Version:** 1.0
**Date:** 2026-04-12
**Status:** Implemented

---

## 1. Executive Summary

Safar's VAS professionals (lawyers, interior designers) were previously seed-data-only — admins had to manually create profiles. This PRD covers a marketplace-style self-registration system where professionals sign up with their own accounts, go live immediately with an "Unverified" badge, and receive a "Verified" badge after admin review. Professionals get a self-service dashboard to manage their profile, view assigned cases/projects, and track earnings.

---

## 2. Design Decision: Why Marketplace Model (Option 3)

Three approaches were evaluated:

| Option | Pros | Cons |
|--------|------|------|
| 1. Public Registration + Admin Gate | Quality control before listing | Slow onboarding, professionals wait |
| 2. Invite-Only | High quality | Limited scale, admin bottleneck |
| **3. Marketplace (chosen)** | **Instant visibility, fastest onboarding, NoBroker-style** | **Unverified profiles visible initially** |

**Rationale:** Option 3 maximizes supply-side growth. Professionals see immediate value (their profile is live within minutes), which drives adoption. The "Unverified" badge manages buyer trust without blocking supply. Admin verification runs asynchronously and adds the trust signal post-facto.

**Risk mitigation:** Unverified profiles are visually distinct (yellow badge vs green badge). Users are informed that unverified professionals haven't been credential-checked. Admin verification queue is prioritized in the admin dashboard (shown first with red badge count).

---

## 3. Registration Flow

### 3.1 Lawyer (Advocate) Registration

**Route:** `/services/legal/register`

**3-Step Form:**

**Step 1 — Personal Details:**
- Full Name (required)
- Bar Council Number
- Email, Phone (required)
- City (required), State
- Office Address

**Step 2 — Experience & Expertise:**
- Years of Experience
- Consultation Fee (INR)
- Specializations (multi-select from 16 options: Property Law, Title Verification, RERA Compliance, Due Diligence, Sale Agreements, Lease Agreements, Landlord-Tenant Disputes, Property Registration, Stamp Duty & Taxation, Construction Law, Encumbrance Check, Family Property Law, NRI Property Law, Agricultural Land Law, Commercial Property Law, Litigation)
- Languages (multi-select: English, Hindi, Telugu, Tamil, Kannada, Marathi, Bengali, Gujarati, Malayalam, Punjabi)
- Bio / About

**Step 3 — Availability & Review:**
- Available Days (MON-FRI / MON-SAT / ALL / WEEKENDS)
- Available Hours (10-6 / 9-5 / 9-9 / FLEXIBLE)
- Registration Summary
- "Register & Go Live" button

### 3.2 Interior Designer Registration

**Route:** `/services/interiors/register`

**3-Step Form:**

**Step 1 — Personal & Company:**
- Full Name (required), Company Name (optional)
- Email, Phone (required)
- City (required), State
- Office/Studio Address

**Step 2 — Expertise & Pricing:**
- Years of Experience
- Consultation Fee (INR)
- Minimum Project Budget (INR)
- IIID Membership (optional)
- Specializations (multi-select from 19 options: Living Room, Bedroom, Kitchen, Bathroom, Modular Kitchen, Wardrobe & Storage, False Ceiling, Flooring, Painting & Wallpaper, Pooja Room, Kids Room, Study Room, Balcony & Terrace, Commercial Office, Retail & Showroom, Restaurant & Cafe, Full Home Interiors, Renovation, Smart Home Integration)
- Bio / About

**Step 3 — Review & Submit:**
- Summary of all entered details
- Post-registration information (portfolio upload, document upload from dashboard)
- "Register & Go Live" button

---

## 4. Verification Flow

### 4.1 Professional Status Lifecycle

```
REGISTRATION → PENDING (live with "Unverified" badge)
                ├── Admin APPROVES → APPROVED (verified=true, green badge)
                └── Admin REJECTS → REJECTED (active=false, hidden from public)
                    └── Professional updates profile → back to PENDING
```

### 4.2 Admin Verification Queue

**Location:** Admin Dashboard → Professionals → "Verification Queue" tab (first tab when pending items exist)

**Queue displays:**
- Pending advocate registrations table (name, city, bar council, experience, phone, email, registration date)
- Pending designer registrations table (name, company, city, experience, phone, registration date)
- Each row has Approve (green) and Reject (red) buttons
- Reject opens modal requiring reason text (displayed to applicant)

**Verification badge count** shown in tab header as red Ant Design Tag.

### 4.3 Kafka Events

| Event | Trigger | Consumer |
|-------|---------|----------|
| `professional.registered` | Self-registration completed | notification-service (welcome email) |
| `professional.approved` | Admin approves | notification-service (congratulations email) |
| `professional.rejected` | Admin rejects with reason | notification-service (rejection email with reason) |

---

## 5. Professional Dashboard

### 5.1 Lawyer Dashboard (`/services/legal/dashboard`)

**Sections:**
1. **Status Banner** — Shows verification status:
   - PENDING: Yellow banner "Profile is live with Unverified badge. Verification in progress."
   - REJECTED: Red banner with rejection reason and "Please update your profile" guidance
   - APPROVED: No banner (clean state)

2. **Profile Card** — View/Edit toggle:
   - View mode: All profile fields displayed (name, bar council, city, experience, fee, email, phone, availability, bio, specializations)
   - Edit mode: Inline form with save button. Professional can edit all fields EXCEPT verificationStatus.

3. **Statistics Sidebar:**
   - Total Cases, Completed Cases, Rating

4. **Quick Links:**
   - "View Public Profile" → `/services/legal/lawyers/{id}`

5. **My Cases** — List of assigned legal cases with status, risk level, link to case detail

### 5.2 Designer Dashboard (`/services/interiors/dashboard`)

Same structure as lawyer dashboard with designer-specific fields:
- Company name, IIID membership, GST number
- Min budget, service areas
- Projects instead of cases
- Quoted amounts shown

---

## 6. Data Model Changes

### Advocate Entity — New Fields

| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | Links to auth-service user account (unique) |
| `verificationStatus` | VARCHAR(20) | PENDING, APPROVED, REJECTED |
| `rejectionReason` | TEXT | Admin-provided reason for rejection |
| `verifiedBy` | UUID | Admin who approved/rejected |
| `verifiedAt` | TIMESTAMPTZ | When verification decision was made |
| `idProofUrl` | TEXT | Uploaded ID proof document |
| `licenseUrl` | TEXT | Uploaded bar council license |
| `certificateUrls` | TEXT | JSON array of certificate URLs |
| `languages` | TEXT | JSON array e.g. ["English","Hindi","Telugu"] |
| `availableDays` | VARCHAR(50) | e.g. "MON-FRI" or "ALL" |
| `availableHours` | VARCHAR(30) | e.g. "10:00-18:00" |

### InteriorDesigner Entity — New Fields

| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | Links to auth-service user account (unique) |
| `verificationStatus` | VARCHAR(20) | PENDING, APPROVED, REJECTED |
| `rejectionReason` | TEXT | Rejection reason |
| `verifiedBy` | UUID | Admin ID |
| `verifiedAt` | TIMESTAMPTZ | Decision timestamp |
| `idProofUrl` | TEXT | ID proof |
| `licenseUrl` | TEXT | Design license |
| `certificateUrls` | TEXT | JSON array |
| `iiidMembership` | VARCHAR(50) | IIID membership number |
| `gstNumber` | VARCHAR(20) | GST registration |
| `serviceAreas` | TEXT | JSON array of cities/localities |
| `minBudgetPaise` | BIGINT | Minimum project budget |

### Migration: `V81__professional_onboarding.sql`
- Adds all new columns with `DEFAULT 'APPROVED'` for existing seed data (so existing advocates/designers remain verified)
- Unique index on `user_id` (partial, WHERE NOT NULL)

---

## 7. API Endpoints

### Self-Registration

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/legal/advocates/register` | User (JWT) | Create advocate profile linked to user |
| POST | `/api/v1/interiors/designers/register` | User (JWT) | Create designer profile linked to user |

**Validation:**
- One profile per userId (400 if already exists)
- Sets `verificationStatus=PENDING`, `verified=false`, `active=true`

### Admin Verification

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/legal/advocates/pending` | Admin | List pending advocate registrations |
| GET | `/api/v1/interiors/designers/pending` | Admin | List pending designer registrations |
| POST | `/api/v1/legal/advocates/{id}/approve` | Admin | Approve → verified=true |
| POST | `/api/v1/legal/advocates/{id}/reject?reason=...` | Admin | Reject → active=false |
| POST | `/api/v1/interiors/designers/{id}/approve` | Admin | Approve |
| POST | `/api/v1/interiors/designers/{id}/reject?reason=...` | Admin | Reject |

### Self-Service Dashboard

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/legal/advocates/my-profile` | User | Get own advocate profile |
| PUT | `/api/v1/legal/advocates/my-profile` | User | Update own profile (partial) |
| GET | `/api/v1/interiors/designers/my-profile` | User | Get own designer profile |
| PUT | `/api/v1/interiors/designers/my-profile` | User | Update own profile (partial) |

**Self-edit restrictions:** Professional can edit all profile fields except `verificationStatus`, `verified`, `verifiedBy`, `verifiedAt`. These are admin-only.

---

## 8. Frontend Pages

### User-Facing (safar-web)

| Page | Route | Description |
|------|-------|-------------|
| Lawyer Registration | `/services/legal/register` | 3-step wizard with specialization chips, language selection, availability picker |
| Designer Registration | `/services/interiors/register` | 3-step wizard with specialization chips, budget setting, IIID membership |
| Lawyer Dashboard | `/services/legal/dashboard` | Profile view/edit, status banner, stats, assigned cases |
| Designer Dashboard | `/services/interiors/dashboard` | Profile view/edit, status banner, stats, assigned projects |

### Admin Dashboard

| Page | Route | Description |
|------|-------|-------------|
| Professionals (enhanced) | `/professionals` | New "Verification Queue" tab with pending count badge, approve/reject buttons, reject reason modal |

---

## 9. User Journey Examples

### New Lawyer Signs Up
```
1. Lawyer visits /services/legal/lawyers → sees "Join as Legal Partner" link
2. Clicks "Register" → /services/legal/register
3. Step 1: Fills name, bar council #, phone, city
4. Step 2: Selects specializations (Property Law, RERA), sets fee ₹500
5. Step 3: Reviews summary → "Register & Go Live"
6. Profile appears in /services/legal/lawyers with yellow "Unverified" badge
7. Admin sees red badge on "Verification Queue" tab
8. Admin reviews → clicks "Approve" → lawyer gets green "Verified" badge
9. Lawyer visits /services/legal/dashboard → sees "Verified" status, assigned cases
```

### Rejected Designer Re-applies
```
1. Designer registered, profile was live
2. Admin rejects: "Missing portfolio, please add project photos"
3. Designer visits /services/interiors/dashboard → sees red rejection banner with reason
4. Designer clicks "Edit Profile" → adds portfolio URLs, updates bio
5. Profile auto-reverts to PENDING (admin re-reviews)
6. Admin approves on second review
```

---

## 10. Files Created/Modified

### Backend

| File | Change |
|------|--------|
| `entity/Advocate.java` | +12 onboarding fields |
| `entity/InteriorDesigner.java` | +12 onboarding fields |
| `repository/AdvocateRepository.java` | +3 query methods (findByUserId, findByVerificationStatus, existsByUserId) |
| `repository/InteriorDesignerRepository.java` | +3 query methods |
| `controller/ProfessionalController.java` | +14 endpoints (register, pending, approve, reject, my-profile) |
| `migration/V81__professional_onboarding.sql` | All new columns for both entities |

### Frontend

| File | Change |
|------|--------|
| `app/services/legal/register/page.tsx` | NEW — 3-step lawyer registration |
| `app/services/interiors/register/page.tsx` | NEW — 3-step designer registration |
| `app/services/legal/dashboard/page.tsx` | NEW — Lawyer self-service dashboard |
| `app/services/interiors/dashboard/page.tsx` | NEW — Designer self-service dashboard |
| `admin/src/pages/ProfessionalsPage.tsx` | Added verification queue tab with approve/reject |
| `lib/api.ts` | +6 new API methods (register, my-profile, update-my-profile for both types) |

---

## 11. Phase 2 (Not in Scope)

| Feature | Notes |
|---------|-------|
| Document upload from dashboard | Currently URL-based; needs S3 upload integration |
| Professional earnings tracking | Requires payment-service integration for consultation fees |
| Automated re-verification after rejected profile update | Currently admin must manually re-check |
| Rating/review system for professionals | `rating` field exists but no user-facing review flow |
| Portfolio management UI (upload/reorder photos) | Designers can set URLs but no drag-drop upload |
| Bank partner self-onboarding | Banks are admin-managed only (different onboarding needs) |
| SMS/email notifications for registration events | Kafka events published but no notification consumers yet |
