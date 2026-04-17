# PRD: VAS Professional Management — Lawyers, Interior Designers & Partner Banks

**Version:** 1.0
**Date:** 2026-04-12
**Status:** Implemented

---

## 1. Executive Summary

This PRD covers the bug fixes, security hardening, and professional management system for Safar's Value-Added Services (VAS): Legal Services, Home Interiors, Home Loans, and Sale Agreements. The work transforms seed-data-only professionals into a fully managed, browsable, and admin-editable system with public profiles and in-app chat.

---

## 2. Problems Identified (Bug Audit)

### 2.1 Critical Security Bugs
- **No admin permission checks** on 7 admin endpoints across LegalController, AgreementController, and InteriorController — anyone could update case statuses, assign advocates, generate reports, and manage agreements without authentication.
- **Fix:** Added `X-User-Role: ADMIN` header validation on all admin-only endpoints.

### 2.2 Data/Logic Bugs

| Bug | Service | Impact | Fix |
|-----|---------|--------|-----|
| Kafka sends UUID object instead of String | AgreementService | `ClassCastException` on downstream consumers | Changed to `id.toString()` |
| Risk level: ANY single issue = RED | LegalService | Over-penalizing properties with minor single issues | Graduated: 2+ issues = RED, 1 = YELLOW, pending = YELLOW |
| Loan eligibility formula: `disposable * 0.5 * tenureMonths * 0.85` | HomeLoanService | Inflated eligible amounts (e.g., 50L income showing 50Cr eligible) | Replaced with proper PV of annuity at 8.5% assumed rate |
| Quote comment/code mismatch: "10% + 10%" vs 20% combined | InteriorService | Developer confusion, audit risk | Fixed comment to match actual calculation |

### 2.3 Missing CRUD Operations

| Entity | Before | After |
|--------|--------|-------|
| Advocates (Lawyers) | List only (seed data) | Full CRUD + public profile |
| Interior Designers | List only (seed data) | Full CRUD + public profile + portfolio |
| Partner Banks | List only (seed data) | Full CRUD + admin management |

---

## 3. Feature: Professional Profile Management

### 3.1 Requirements

**Admin capabilities:**
- Add new lawyers, interior designers, and partner banks from the admin dashboard
- Edit all profile fields (name, contact, bio, specializations, fees, verified status)
- Soft-delete (deactivate) professionals — they disappear from public listings but data is preserved
- View all professionals in a tabbed interface with search and status indicators

**Public-facing capabilities:**
- Browse lawyers by city with card grid (name, bar council, experience, rating, specializations, consultation fee)
- Browse interior designers by city with portfolio preview images
- View full lawyer/designer profile with bio, specializations, contact info
- Start a legal case or interior project directly from the professional's profile
- Chat with the professional via in-app messaging

### 3.2 Data Model

**Advocate (Lawyer) — already exists, now fully managed:**
- `fullName`, `barCouncilId`, `email`, `phone`, `address`, `city`, `state`
- `experienceYears`, `specializations[]`, `profilePhotoUrl`, `rating`, `bio`
- `totalCases`, `completedCases`, `consultationFeePaise`
- `verified` (admin-set), `active` (soft-delete flag)

**InteriorDesigner — already exists, now fully managed:**
- `fullName`, `companyName`, `email`, `phone`, `address`, `city`, `state`
- `experienceYears`, `specializations[]`, `portfolioUrls[]`, `profilePhotoUrl`, `rating`, `bio`
- `totalProjects`, `completedProjects`, `consultationFeePaise`
- `verified`, `active`

**PartnerBank — already exists, now fully managed:**
- `bankName`, `logoUrl`, `interestRateMin/Max`, `maxTenureMonths`, `maxLoanAmountPaise`
- `processingFeePercent/Min/Max`, `preApprovalAvailable`, `balanceTransferAvailable`
- `contactName`, `contactEmail`, `contactPhone`, `specialOffers`
- `active`, `sortOrder`

### 3.3 API Endpoints

**ProfessionalController** — 12 new endpoints:

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/legal/advocates` | Public | List active advocates (optional city filter) |
| GET | `/api/v1/legal/advocates/{id}` | Public | Get single advocate profile |
| POST | `/api/v1/legal/advocates` | Admin | Create new advocate |
| PUT | `/api/v1/legal/advocates/{id}` | Admin | Update advocate (partial update) |
| DELETE | `/api/v1/legal/advocates/{id}` | Admin | Soft-delete (sets active=false) |
| GET | `/api/v1/interiors/designers` | Public | List active designers (optional city filter) |
| GET | `/api/v1/interiors/designers/{id}` | Public | Get single designer profile |
| POST | `/api/v1/interiors/designers` | Admin | Create new designer |
| PUT | `/api/v1/interiors/designers/{id}` | Admin | Update designer |
| DELETE | `/api/v1/interiors/designers/{id}` | Admin | Soft-delete |
| GET | `/api/v1/homeloan/banks/{id}` | Public | Get single bank details |
| POST | `/api/v1/homeloan/banks` | Admin | Create new bank |
| PUT | `/api/v1/homeloan/banks/{id}` | Admin | Update bank |
| DELETE | `/api/v1/homeloan/banks/{id}` | Admin | Soft-delete |

### 3.4 Frontend Pages

**User-facing (safar-web):**

| Page | Route | Key Features |
|------|-------|-------------|
| Lawyer Browse | `/services/legal/lawyers` | City pill filter, card grid (photo, name, bar council, experience, cases, rating, specializations, consultation fee), click to profile |
| Lawyer Profile | `/services/legal/lawyers/[id]` | Full profile header (photo, name, verified badge, bar council, location, experience, cases, rating), bio section, specializations tags, sidebar with consultation fee + "Start Legal Case" CTA + "Chat" button |
| Designer Browse | `/services/interiors/designers` | City pill filter, card grid with portfolio preview image, name, company, experience, projects, rating, specializations |
| Designer Profile | `/services/interiors/designers/[id]` | Full profile, portfolio gallery (clickable image grid), specializations, sidebar with consultation fee + "Start Interior Project" CTA + "Chat" button |

**Admin (admin dashboard):**

| Page | Route | Key Features |
|------|-------|-------------|
| Professionals | `/professionals` | 3 tabs (Advocates, Designers, Banks), table with status tags, add/edit modal with form fields, soft-delete with confirmation |

### 3.5 User Flows

**Admin adds a new lawyer:**
```
Admin → /professionals → Advocates tab → "Add Advocate"
→ Fill form (name, bar council, city, experience, specializations, fee, bio)
→ Save → appears in table → mark as Verified → appears on public listing
```

**User finds and contacts a lawyer:**
```
User → /services/legal → "Our Legal Partners" link
→ /services/legal/lawyers → filter by city → click lawyer card
→ /services/legal/lawyers/[id] → review profile, specializations, rating
→ "Start Legal Case" or "Chat" → redirected to case creation or messaging
```

**Admin deactivates a designer:**
```
Admin → /professionals → Designers tab → click delete icon
→ Confirm → designer marked active=false → disappears from public listing
→ Can be reactivated by editing and setting active=true
```

---

## 4. Security Hardening

### 4.1 Admin Permission Checks Added

| Controller | Endpoints Secured |
|------------|-------------------|
| LegalController | `GET /cases` (admin list), `PATCH /cases/{id}/status`, `POST /cases/{id}/assign`, `POST /cases/{id}/generate-report` |
| AgreementController | `GET /admin/list`, `PATCH /{id}/status` |
| InteriorController | `GET /admin/projects`, `POST /projects/{id}/designer` |
| ProfessionalController | All POST, PUT, DELETE endpoints |

**Mechanism:** `@RequestHeader("X-User-Role")` validated against `"ADMIN"`. Returns `403 AccessDeniedException` if not admin. The API Gateway's `JwtAuthFilter` propagates the role from JWT claims.

---

## 5. Logic Bug Fixes

### 5.1 Loan Eligibility Formula (HomeLoanService)

**Before (wrong):**
```
maxEligible = (monthlyIncome - currentEmis) * 0.5 * tenureMonths * 0.85
```
Problem: Multiplying by tenure months (e.g., 240) inflates the eligible amount unrealistically.

**After (correct):**
```
maxEmi = disposableIncome * 0.5  (FOIR = 50%)
maxEligible = PV of annuity at 8.5% p.a. over tenure
            = maxEmi × [(1+r)^n - 1] / [r × (1+r)^n]
```
This matches standard Indian banking practice (FOIR-based eligibility with PV discounting).

### 5.2 Legal Risk Level (LegalService)

**Before:** Any single `ISSUE_FOUND` verification → RED risk.
**After:** Graduated scale:
- All CLEAN → GREEN
- 2+ issues → RED (high risk)
- 1 issue → YELLOW (medium risk, investigate further)
- Pending verifications → YELLOW (incomplete, can't determine)

### 5.3 Kafka Message Serialization (AgreementService)

**Before:** `kafkaTemplate.send("topic", id.toString(), id)` — sends UUID object as value.
**After:** `kafkaTemplate.send("topic", id.toString(), id.toString())` — sends String.

---

## 6. Files Modified/Created

### Backend (listing-service)

| File | Change |
|------|--------|
| **NEW:** `controller/ProfessionalController.java` | 12 CRUD endpoints for advocates, designers, banks |
| `controller/LegalController.java` | Admin permission checks, removed duplicate `/advocates` endpoint |
| `controller/AgreementController.java` | Admin permission checks on list + status update |
| `controller/InteriorController.java` | Admin permission checks on projects + designer assignment |
| `service/AgreementService.java` | Fixed Kafka serialization (lines 191, 222) |
| `service/LegalService.java` | Fixed risk level logic (graduated scale) |
| `service/HomeLoanService.java` | Fixed eligibility formula (PV of annuity) |
| `service/InteriorService.java` | Fixed quote cost comment |

### Frontend (safar-web)

| File | Description |
|------|-------------|
| **NEW:** `app/services/legal/lawyers/page.tsx` | Lawyer browse page with city filter |
| **NEW:** `app/services/legal/lawyers/[id]/page.tsx` | Lawyer profile with CTA + chat |
| **NEW:** `app/services/interiors/designers/page.tsx` | Designer browse page with portfolio preview |
| **NEW:** `app/services/interiors/designers/[id]/page.tsx` | Designer profile with gallery + CTA |
| `lib/api.ts` | 12 new API methods for professional CRUD |

### Admin Dashboard

| File | Description |
|------|-------------|
| **NEW:** `admin/src/pages/ProfessionalsPage.tsx` | 3-tab management page with Ant Design tables + modals |
| `admin/src/App.tsx` | Added `/professionals` route |
| `admin/src/components/AdminLayout.tsx` | Added "Professionals" sidebar link |

---

## 7. Remaining Gaps (Phase 2)

| Gap | Priority | Notes |
|-----|----------|-------|
| Agreement DELETE endpoint | Medium | Cancel/void agreement flow |
| Legal case DELETE endpoint | Medium | Close/archive case |
| Document upload for legal/interior | Medium | Currently URL-based, needs S3 upload |
| Payment integration for VAS | High | Razorpay for legal/interior/agreement payments |
| Email notifications for VAS status changes | Medium | Kafka events exist but no notification consumers |
| Advocate/Designer user accounts | Low | Currently admin-managed profiles, no self-registration |
| Rating/review system for professionals | Low | `rating` field exists but no user-facing review flow |
| Expanded seed data (more cities) | Low | Currently 10 cities for advocates, 5 designers |
