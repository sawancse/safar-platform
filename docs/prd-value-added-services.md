# PRD: Safar Value-Added Services (VAS)
## Sale Agreement | Home Loan | Legal Services | Home Interiors

**Version:** 1.0
**Date:** 2026-04-04
**Status:** Design & PRD
**Reference:** NoBroker.in VAS model, adapted for Safar India MVP

---

## 1. Executive Summary

Four value-added services to monetize the Safar buy/sell marketplace beyond commission. These services address the full property transaction lifecycle: legal documentation, financing, verification, and post-purchase interiors.

**Revenue potential:** ~INR 15,000-80,000 per transaction across services.

---

## 2. Service 1: Sale Agreement

### 2.1 Overview
Online generation of legally binding sale/rental agreements with e-stamping, e-signing (Aadhaar), and doorstep delivery. Covers sale deed, sale agreement, and rental agreements.

### 2.2 User Journey

```
Select Agreement Type → Enter Party Details → Enter Property Details
→ Choose Clauses/Customizations → Preview Draft → Pay
→ E-Stamp Processing → Aadhaar E-Sign (both parties)
→ Registration (optional SRO assistance) → Download/Delivery
```

### 2.3 Agreement Types
| Type | Use Case | Stamp Duty |
|------|----------|------------|
| SALE_AGREEMENT | Agreement to sell (pre-registration) | State-specific % |
| SALE_DEED | Final transfer document | State-specific % |
| RENTAL_AGREEMENT | 11-month rental | Varies by state |
| LEAVE_LICENSE | Leave and license (Maharashtra) | 0.25% of deposit |
| PG_AGREEMENT | PG tenancy agreement | Minimal |

### 2.4 Pricing Tiers
| Package | Price | Includes |
|---------|-------|----------|
| Basic Draft | Free | PDF draft only (watermarked) |
| E-Stamp + E-Sign | INR 1,499 | E-stamped + Aadhaar e-signed |
| Registered | INR 4,999 | Above + SRO registration assistance |
| Premium | INR 9,999 | Above + title verification + doorstep delivery |

### 2.5 Key Screens
1. **Agreement Type Selector** — Cards with type, description, estimated cost
2. **Party Details Form** — Buyer/seller name, Aadhaar, PAN, address, phone
3. **Property Details Form** — Address, survey number, area, value, linked listing/sale-property
4. **Clause Customizer** — Toggle standard clauses, add custom clauses, preview
5. **Draft Preview** — Full agreement preview with edit capability
6. **Payment & Processing** — Razorpay checkout, stamp duty calculator
7. **E-Sign Flow** — Aadhaar OTP verification for each party
8. **Status Tracker** — Timeline: Draft → Stamped → Signed → Registered → Delivered
9. **My Agreements** — Dashboard listing all user agreements with status

### 2.6 Backend Design

#### Entities
```
AgreementRequest
  - id (UUID)
  - userId (UUID) — creator
  - agreementType (enum)
  - propertyId (UUID, nullable) — linked sale property
  - listingId (UUID, nullable) — linked rental listing
  - status (DRAFT/STAMPED/PENDING_SIGN/SIGNED/PENDING_REGISTRATION/REGISTERED/DELIVERED)
  - partyDetailsJson (JSON) — buyer/seller details
  - propertyDetailsJson (JSON) — address, area, value
  - clausesJson (JSON) — selected/custom clauses
  - stampDutyPaise (Long)
  - serviceFeesPaise (Long)
  - packageType (BASIC/ESTAMP/REGISTERED/PREMIUM)
  - draftPdfUrl (String)
  - signedPdfUrl (String)
  - registeredPdfUrl (String)
  - eStampId (String)
  - registrationNumber (String)
  - paymentId (UUID)
  - createdAt, updatedAt

AgreementParty
  - id (UUID)
  - agreementId (UUID)
  - partyType (BUYER/SELLER/TENANT/LANDLORD/WITNESS)
  - fullName, aadhaarNumber, panNumber
  - address, phone, email
  - eSignStatus (PENDING/SIGNED/REJECTED)
  - eSignedAt (OffsetDateTime)

StampDutyConfig
  - id (UUID)
  - state (String)
  - agreementType (enum)
  - dutyPercent (BigDecimal)
  - registrationPercent (BigDecimal)
  - surchargePercent (BigDecimal)
  - effectiveFrom (LocalDate)
```

#### API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/agreements | Create agreement request |
| GET | /api/v1/agreements/{id} | Get agreement details |
| GET | /api/v1/agreements/my | List user's agreements |
| PUT | /api/v1/agreements/{id} | Update draft |
| POST | /api/v1/agreements/{id}/generate-draft | Generate PDF draft |
| GET | /api/v1/agreements/{id}/draft-pdf | Download draft PDF |
| POST | /api/v1/agreements/{id}/pay | Initiate payment |
| POST | /api/v1/agreements/{id}/stamp | Process e-stamping |
| POST | /api/v1/agreements/{id}/sign | Initiate e-sign for a party |
| POST | /api/v1/agreements/{id}/sign/verify | Verify Aadhaar OTP for sign |
| GET | /api/v1/agreements/{id}/signed-pdf | Download signed document |
| GET | /api/v1/agreements/stamp-duty/calculate | Calculate stamp duty by state |
| GET | /api/v1/agreements/templates | List agreement templates |

#### Kafka Events
- `agreement.created`, `agreement.paid`, `agreement.stamped`, `agreement.signed`, `agreement.registered`

---

## 3. Service 2: Home Loan Marketplace

### 3.1 Overview
Multi-bank home loan comparison and application platform. Free for users; Safar earns commission from partner banks on successful disbursements.

### 3.2 User Journey

```
Check Eligibility (income, EMIs, loan amount)
→ View Matched Banks (interest rates, tenure, fees)
→ Compare Side-by-Side → Select Bank → Apply
→ Upload Documents → Track Application
→ Sanction Letter → Disbursement
```

### 3.3 Revenue Model
- **Commission from banks:** 0.5-1.5% of disbursed loan amount
- **Free for users** — drives traffic to paid services
- **Cross-sell:** Offer legal services + interiors to approved borrowers

### 3.4 Key Screens
1. **Eligibility Calculator** — Employment type, income, existing EMIs, desired loan amount/tenure
2. **EMI Calculator** — Loan amount slider, rate, tenure → monthly EMI, total interest, total cost
3. **Bank Comparison** — Table: bank logo, rate, processing fee, max tenure, max LTV, special offers
4. **Bank Detail Card** — Expanded view with eligibility criteria, documents needed, turnaround
5. **Application Form** — Pre-filled from eligibility check, additional details
6. **Document Upload** — Checklist with upload slots: ID proof, income proof, property docs, bank statements
7. **Application Tracker** — Status: Applied → Verified → Sanctioned → Disbursed
8. **My Applications** — Dashboard of all loan applications

### 3.5 Backend Design

#### Entities
```
LoanEligibility
  - id (UUID)
  - userId (UUID)
  - employmentType (SALARIED/SELF_EMPLOYED/BUSINESS)
  - monthlyIncomePaise (Long)
  - currentEmisPaise (Long)
  - desiredLoanAmountPaise (Long)
  - desiredTenureMonths (Integer)
  - maxEligibleAmountPaise (Long)
  - calculatedAt (OffsetDateTime)

PartnerBank
  - id (UUID)
  - bankName, logoUrl
  - minInterestRate, maxInterestRate (BigDecimal)
  - processingFeePercent (BigDecimal)
  - maxTenureMonths (Integer)
  - maxLtvPercent (Integer) — e.g. 90
  - minLoanAmountPaise, maxLoanAmountPaise (Long)
  - minIncomePaise (Long)
  - specialOffers (String)
  - commissionPercent (BigDecimal) — Safar's earning
  - active (Boolean)

LoanApplication
  - id (UUID)
  - userId (UUID)
  - eligibilityId (UUID)
  - bankId (UUID)
  - loanAmountPaise (Long)
  - tenureMonths (Integer)
  - interestRate (BigDecimal)
  - status (APPLIED/DOCUMENTS_PENDING/UNDER_REVIEW/SANCTIONED/DISBURSED/REJECTED)
  - sanctionedAmountPaise (Long)
  - sanctionLetterUrl (String)
  - appliedAt, updatedAt

LoanDocument
  - id (UUID)
  - applicationId (UUID)
  - documentType (ID_PROOF/INCOME_PROOF/BANK_STATEMENT/PROPERTY_DOCS/ITR/FORM16)
  - fileUrl (String)
  - verificationStatus (PENDING/VERIFIED/REJECTED)
  - uploadedAt
```

#### API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/homeloan/eligibility | Calculate eligibility |
| GET | /api/v1/homeloan/eligibility/{id} | Get eligibility result |
| POST | /api/v1/homeloan/emi/calculate | Calculate EMI |
| GET | /api/v1/homeloan/banks | List partner banks |
| GET | /api/v1/homeloan/banks/{id} | Bank details |
| GET | /api/v1/homeloan/banks/compare | Compare matched banks for user |
| POST | /api/v1/homeloan/apply | Submit loan application |
| GET | /api/v1/homeloan/applications/my | User's applications |
| GET | /api/v1/homeloan/applications/{id} | Application details |
| POST | /api/v1/homeloan/applications/{id}/documents | Upload document |
| GET | /api/v1/homeloan/applications/{id}/documents | List documents |

---

## 4. Service 3: Legal Services

### 4.1 Overview
Professional property verification, title search, and legal opinion services. Expert advocates examine documents and government records to assess risk before property purchase.

### 4.2 User Journey

```
Select Package → Upload Property Documents
→ Advocate Assigned → Document Analysis (3-5 days)
→ Government Record Verification → Risk Assessment
→ Legal Opinion Report → Expert Consultation (optional)
```

### 4.3 Pricing Tiers
| Package | Price | TAT | Includes |
|---------|-------|-----|----------|
| Title Search | INR 2,999 | 5 days | Title chain verification, ownership history |
| Due Diligence | INR 9,999 | 8 days | Title + encumbrance + govt approvals + tax |
| Buyer Assist | INR 19,999 | 15 days | Above + agreement draft + SRO + khata |
| Premium | INR 49,999 | 20 days | Above + unlimited consultations + name transfer |

### 4.4 Key Screens
1. **Package Selector** — Cards with features, price, turnaround time
2. **Document Upload** — Checklist: title deed, sale deed, tax receipts, EC, khata, approvals
3. **Case Dashboard** — Status timeline, assigned advocate, estimated completion
4. **Verification Progress** — Checklist items going green as verified
5. **Risk Assessment Report** — Color-coded findings (GREEN/YELLOW/RED), recommendations
6. **Legal Opinion** — Downloadable PDF with advocate signature
7. **Consultation Scheduler** — Book call with assigned advocate
8. **My Cases** — List of all legal service requests

### 4.5 Backend Design

#### Entities
```
LegalCase
  - id (UUID)
  - userId (UUID)
  - propertyId (UUID, nullable) — linked sale property
  - packageType (TITLE_SEARCH/DUE_DILIGENCE/BUYER_ASSIST/PREMIUM)
  - status (CREATED/DOCUMENTS_UPLOADED/ASSIGNED/IN_PROGRESS/REPORT_READY/CONSULTATION_DONE/CLOSED)
  - advocateId (UUID, nullable)
  - propertyAddress (String)
  - propertyCity, propertyState (String)
  - surveyNumber (String)
  - paymentId (UUID)
  - reportPdfUrl (String)
  - riskLevel (GREEN/YELLOW/RED, nullable)
  - findingsJson (JSON)
  - dueDate (LocalDate)
  - createdAt, updatedAt

LegalDocument
  - id (UUID)
  - caseId (UUID)
  - documentType (TITLE_DEED/SALE_DEED/TAX_RECEIPT/EC/KHATA/APPROVAL/PLAN/OTHER)
  - fileUrl (String)
  - verificationStatus (PENDING/VERIFIED/ISSUE_FOUND/NOT_AVAILABLE)
  - remarks (String)
  - uploadedAt

LegalVerification
  - id (UUID)
  - caseId (UUID)
  - verificationType (TITLE_CHAIN/ENCUMBRANCE/GOVT_APPROVAL/LITIGATION/TAX/SURVEY)
  - status (PENDING/IN_PROGRESS/CLEAN/ISSUE_FOUND)
  - findingsJson (JSON)
  - verifiedAt

Advocate
  - id (UUID)
  - fullName, phone, email
  - barCouncilNumber (String)
  - specializations (String[])
  - city, state
  - casesCompleted (Integer)
  - rating (BigDecimal)
  - active (Boolean)

LegalConsultation
  - id (UUID)
  - caseId (UUID)
  - advocateId (UUID)
  - scheduledAt (OffsetDateTime)
  - durationMinutes (Integer)
  - meetingUrl (String)
  - notes (String)
  - status (SCHEDULED/COMPLETED/CANCELLED)
```

#### API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/legal/cases | Create legal case |
| GET | /api/v1/legal/cases/my | User's cases |
| GET | /api/v1/legal/cases/{id} | Case details with verifications |
| POST | /api/v1/legal/cases/{id}/documents | Upload document |
| GET | /api/v1/legal/cases/{id}/documents | List case documents |
| GET | /api/v1/legal/cases/{id}/report | Download risk report PDF |
| POST | /api/v1/legal/cases/{id}/consultation | Schedule consultation |
| GET | /api/v1/legal/packages | List available packages |
| GET | /api/v1/legal/stamp-duty/{state} | State stamp duty rates |
| PATCH | /api/v1/legal/cases/{id}/status | Admin: update case status |
| POST | /api/v1/legal/cases/{id}/assign | Admin: assign advocate |

---

## 5. Service 4: Home Interiors

### 5.1 Overview
End-to-end interior design and execution service. Users get free consultation, 3D visualization, and professional execution with quality guarantees. Safar earns margin on materials + labor.

### 5.2 User Journey

```
Book Free Consultation → Designer Visit & Measurement
→ 3D Design & Floor Plan → Review & Approve
→ Material Selection → Quote Approval → Pay Advance
→ Execution (4-8 weeks) → Quality Checks → Handover
```

### 5.3 Service Packages
| Package | Scope | Price Range |
|---------|-------|-------------|
| Modular Kitchen | Kitchen cabinets, countertop, backsplash | INR 1.5L - 5L |
| Wardrobe | Built-in wardrobes, walk-in closets | INR 80K - 3L |
| Full Room | Single room complete makeover | INR 2L - 6L |
| Full Home | 2-3 BHK complete interiors | INR 5L - 20L |
| Renovation | Structural changes + interiors | INR 8L - 30L |

### 5.4 Key Screens
1. **Consultation Booking** — Select city, property type, rooms, budget range, preferred date
2. **Designer Profiles** — Browse designers with portfolio, rating, experience, specialization
3. **Project Dashboard** — Timeline, current phase, assigned team, next milestone
4. **3D Visualization Gallery** — Room-by-room 3D renders, rotate/zoom, before/after
5. **Material Catalog** — Browse finishes, materials, colors with pricing
6. **Quote Breakdown** — Itemized: materials, labor, hardware, overhead, total
7. **Payment Schedule** — Milestone-based payments: 30% advance, 40% mid, 30% handover
8. **Progress Tracker** — Photos, milestone updates, quality check results
9. **Quality Report** — 250-point checklist with pass/fail for each item
10. **Review & Handover** — Final walkthrough, warranty card, maintenance guide
11. **My Projects** — All interior projects with status

### 5.5 Backend Design

#### Entities
```
InteriorProject
  - id (UUID)
  - userId (UUID)
  - designerId (UUID, nullable)
  - projectManagerId (UUID, nullable)
  - projectType (MODULAR_KITCHEN/WARDROBE/FULL_ROOM/FULL_HOME/RENOVATION)
  - propertyType (APARTMENT/VILLA/INDEPENDENT_HOUSE)
  - propertyAddress, city, state, pincode
  - roomCount (Integer)
  - budgetMinPaise, budgetMaxPaise (Long)
  - quotedAmountPaise (Long, nullable)
  - status (CONSULTATION_BOOKED/MEASUREMENT_DONE/DESIGN_IN_PROGRESS/DESIGN_APPROVED/MATERIAL_SELECTED/QUOTE_APPROVED/EXECUTION/QC_IN_PROGRESS/COMPLETED/CANCELLED)
  - consultationDate (LocalDate)
  - startDate, estimatedCompletionDate, actualCompletionDate
  - warrantyExpiresAt (LocalDate)
  - createdAt, updatedAt

InteriorDesigner
  - id (UUID)
  - fullName, phone, email
  - city (String)
  - experienceYears (Integer)
  - specializations (String[])
  - portfolioUrls (String[])
  - projectsCompleted (Integer)
  - rating (BigDecimal)
  - active (Boolean)

RoomDesign
  - id (UUID)
  - projectId (UUID)
  - roomType (LIVING/BEDROOM/KITCHEN/BATHROOM/DINING/STUDY/BALCONY)
  - areaSqft (Integer)
  - designStyle (MODERN/CONTEMPORARY/TRADITIONAL/MINIMALIST/INDUSTRIAL)
  - render3dUrl (String)
  - floorPlanUrl (String)
  - moodBoardUrl (String)
  - currentPhotos (String[])
  - status (PENDING/DESIGNED/APPROVED/EXECUTED)

MaterialSelection
  - id (UUID)
  - projectId (UUID)
  - roomDesignId (UUID)
  - category (FLOORING/WALL/COUNTERTOP/CABINET/HARDWARE/LIGHTING/PAINT)
  - materialName (String)
  - brand (String)
  - finish (String)
  - unitPricePaise (Long)
  - quantity (Integer)
  - totalPricePaise (Long)

InteriorQuote
  - id (UUID)
  - projectId (UUID)
  - version (Integer)
  - materialCostPaise (Long)
  - laborCostPaise (Long)
  - hardwareCostPaise (Long)
  - overheadPaise (Long)
  - discountPaise (Long)
  - totalPaise (Long)
  - validUntil (LocalDate)
  - status (DRAFT/SENT/APPROVED/EXPIRED)
  - quoteDocUrl (String)

ProjectMilestone
  - id (UUID)
  - projectId (UUID)
  - milestoneName (String)
  - description (String)
  - scheduledDate (LocalDate)
  - completedDate (LocalDate)
  - status (PENDING/IN_PROGRESS/COMPLETED/DELAYED)
  - photos (String[])
  - paymentLinked (Boolean)
  - paymentAmountPaise (Long)

QualityCheck
  - id (UUID)
  - projectId (UUID)
  - milestoneId (UUID)
  - checkpointName (String)
  - category (MATERIAL/FINISH/ALIGNMENT/HARDWARE/ELECTRICAL/PLUMBING)
  - status (PASS/FAIL/REWORK)
  - inspectorNotes (String)
  - photos (String[])
  - inspectedAt (OffsetDateTime)
```

#### API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/interiors/consultation | Book consultation |
| GET | /api/v1/interiors/designers | List designers by city |
| GET | /api/v1/interiors/designers/{id} | Designer profile + portfolio |
| POST | /api/v1/interiors/projects | Create project |
| GET | /api/v1/interiors/projects/my | User's projects |
| GET | /api/v1/interiors/projects/{id} | Project details |
| POST | /api/v1/interiors/projects/{id}/rooms | Add room design |
| GET | /api/v1/interiors/projects/{id}/rooms | List room designs |
| POST | /api/v1/interiors/projects/{id}/rooms/{roomId}/approve | Approve room design |
| POST | /api/v1/interiors/projects/{id}/materials | Add material selection |
| GET | /api/v1/interiors/materials/catalog | Browse material catalog |
| POST | /api/v1/interiors/projects/{id}/quote | Generate/update quote |
| POST | /api/v1/interiors/projects/{id}/quote/approve | Approve quote |
| GET | /api/v1/interiors/projects/{id}/milestones | List milestones |
| POST | /api/v1/interiors/projects/{id}/milestones/{mid}/complete | Mark milestone done |
| GET | /api/v1/interiors/projects/{id}/quality-checks | List QC results |
| POST | /api/v1/interiors/projects/{id}/handover | Complete handover |
| POST | /api/v1/interiors/projects/{id}/review | Submit review |

---

## 6. Database Schema Summary

### New Service: `listing-service` extensions (or new `vas-service`)

| Table | Service | Est. Rows (Year 1) |
|-------|---------|---------------------|
| agreement_requests | Sale Agreement | 10K |
| agreement_parties | Sale Agreement | 30K |
| stamp_duty_configs | Sale Agreement | 100 |
| loan_eligibilities | Home Loan | 50K |
| partner_banks | Home Loan | 20 |
| loan_applications | Home Loan | 15K |
| loan_documents | Home Loan | 60K |
| legal_cases | Legal | 5K |
| legal_documents | Legal | 25K |
| legal_verifications | Legal | 30K |
| advocates | Legal | 200 |
| legal_consultations | Legal | 3K |
| interior_projects | Interiors | 2K |
| interior_designers | Interiors | 100 |
| room_designs | Interiors | 8K |
| material_selections | Interiors | 20K |
| interior_quotes | Interiors | 4K |
| project_milestones | Interiors | 10K |
| quality_checks | Interiors | 50K |

### Flyway Migration Plan
- V70: agreement_requests + agreement_parties + stamp_duty_configs
- V71: loan_eligibilities + partner_banks + loan_applications + loan_documents
- V72: legal_cases + legal_documents + legal_verifications + advocates + legal_consultations
- V73: interior_projects + interior_designers + room_designs + material_selections + interior_quotes + project_milestones + quality_checks

---

## 7. Frontend Page Map

### Web (safar-web)
| Route | Page | Service |
|-------|------|---------|
| /services | VAS landing page (4 service cards) | All |
| /services/agreement | Agreement type selector | Sale Agreement |
| /services/agreement/new | Multi-step agreement wizard | Sale Agreement |
| /services/agreement/[id] | Agreement detail + status tracker | Sale Agreement |
| /services/agreement/my | My agreements list | Sale Agreement |
| /services/homeloan | Home loan landing + eligibility calc | Home Loan |
| /services/homeloan/compare | Bank comparison matrix | Home Loan |
| /services/homeloan/apply/[bankId] | Application form | Home Loan |
| /services/homeloan/my | My applications | Home Loan |
| /services/legal | Legal services landing + package cards | Legal |
| /services/legal/new | Case creation + document upload | Legal |
| /services/legal/[id] | Case dashboard + report | Legal |
| /services/legal/my | My cases | Legal |
| /services/interiors | Interiors landing + booking | Interiors |
| /services/interiors/designers | Designer gallery | Interiors |
| /services/interiors/[id] | Project dashboard | Interiors |
| /services/interiors/my | My projects | Interiors |

### Admin (admin portal)
| Route | Page |
|-------|------|
| /agreements | Manage agreements, approve registrations |
| /homeloan | View applications, bank commission tracking |
| /legal-cases | Manage cases, assign advocates, upload reports |
| /interiors | Manage projects, assign designers, QC tracking |

---

## 8. Implementation Priority

### Phase 1 (Sprint 1-2): Sale Agreement + Home Loan Calculator
- Sale Agreement: Backend entities + wizard + PDF generation + payment
- Home Loan: EMI calculator + eligibility calculator + bank comparison (static data)
- VAS landing page with 4 service cards

### Phase 2 (Sprint 3-4): Legal Services + Loan Applications
- Legal: Case management + document upload + admin assignment + report generation
- Home Loan: Full application flow + document upload + status tracking

### Phase 3 (Sprint 5-6): Home Interiors
- Interiors: Consultation booking + project management + quote + milestone tracking
- Designer profiles + material catalog

### Phase 4 (Sprint 7+): Advanced Features
- E-stamping integration (SHCIL API)
- Aadhaar e-sign integration (eSign API)
- Bank API integrations for real-time offers
- 3D visualization integration
- Quality check mobile app for inspectors

---

## 9. Technical Architecture

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐
│  safar-web   │────>│ api-gateway   │────>│ listing-service│ (agreements, legal)
│  (Next.js)   │     │  (8080)       │     │  (8083)        │
└─────────────┘     └──────────────┘     ├───────────────┤
                                          │ payment-service│ (Razorpay)
                                          │  (8086)        │
                                          ├───────────────┤
                                          │ notification   │ (emails, SMS)
                                          │  (8089)        │
                                          ├───────────────┤
                                          │ media-service  │ (doc uploads)
                                          │  (8088)        │
                                          └───────────────┘

New Kafka Topics:
- agreement.created / agreement.paid / agreement.signed
- loan.application.created / loan.application.status
- legal.case.created / legal.case.report-ready
- interior.project.created / interior.milestone.completed
```

---

## 10. Success Metrics

| Metric | Target (Year 1) |
|--------|-----------------|
| Agreement requests/month | 500+ |
| Home loan applications/month | 200+ |
| Legal cases/month | 100+ |
| Interior projects/month | 30+ |
| VAS revenue/month | INR 25L+ |
| User satisfaction (NPS) | > 60 |
| Repeat usage rate | > 30% |
