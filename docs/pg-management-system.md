# Safar PG Management System — Complete Documentation

## Overview
End-to-end Paying Guest (PG) / Hostel management system inspired by Zolo PG. Covers the full lifecycle: tenant onboarding, rental agreement, recurring rent, utility billing, maintenance, move-out settlement, and host-admin commission payouts.

---

## Architecture

### Services Involved
| Service | Port | Role |
|---------|------|------|
| booking-service | 8095 | Tenancy, agreements, invoices, settlements, utilities, maintenance |
| payment-service | 8086 | Razorpay subscriptions, host payouts, commission settlement |
| listing-service | 8083 | PG listing config (grace period, penalty, packages) |
| user-service | 8092 | Host KYC, bank details, Razorpay linked account |
| api-gateway | 8080 | Route all `/api/v1/pg-tenancies/**` and `/api/v1/payments/**` |

### Database Schemas
| Schema | Tables Added |
|--------|-------------|
| bookings | tenancy_agreements, tenancy_settlements, settlement_deductions, utility_readings, maintenance_requests |
| payments | tenancy_subscriptions, host_payouts |
| listings | ALTER listings (grace_period_days, late_penalty_bps) |
| users | ALTER host_kyc (razorpay_linked_account_id) |

---

## End-to-End Flow

```
1. Host creates PG listing
   → Sets: monthly rent, security deposit, grace period (default 5 days), late penalty (default 2%/day), max penalty cap (25%)
   → Configures PG packages (Basic/Premium with meals, wifi, laundry)

2. Tenant books PG
   → POST /api/v1/pg-tenancies (CreateTenancyRequest)
   → Inherits penalty config: request value > listing value > default
   → Status: ACTIVE, billingDay set, nextBillingDate calculated

3. Rental Agreement
   → POST /api/v1/pg-tenancies/{id}/agreement — auto-generates Indian rental agreement
   → Host signs → PENDING_TENANT_SIGN
   → Tenant signs → ACTIVE
   → PDF available for download (OpenPDF)

4. Razorpay Subscription (Auto-Debit)
   → POST /api/v1/payments/tenancy/{id}/subscription
   → Razorpay creates plan + subscription
   → Tenant authenticates e-mandate on Razorpay checkout
   → Monthly auto-debit begins

5. Monthly Billing Cycle
   → 6 AM daily: TenancyBillingScheduler generates invoices
   → Invoice includes: rent + packages + electricity + water + 18% GST
   → Unbilled utility readings auto-included and marked as billed
   → Razorpay subscription.charged webhook → invoice marked PAID

6. Host Commission Payout (on each rent payment)
   → subscription.charged → booking-service publishes `tenancy.rent.collected`
   → payment-service creates HostPayout:
     - Gross amount (full rent from tenant)
     - Commission: gross × tier rate (STARTER 18%, PRO 12%, COMMERCIAL 10%, MEDICAL 8%, AASHRAY 0%)
     - GST on commission: 18%
     - TDS: 1% if monthly rent > ₹50,000 (u/s 194-IB)
     - Net payout = gross - commission - GST - TDS
   → Razorpay Route transfer to host's linked bank account

7. Late Payment Handling
   → 7 AM daily: applyLatePenalties()
   → After grace period (default 5 days), penalty accrues daily
   → Penalty = invoice total × latePenaltyBps / 10000 × days overdue
   → Capped at maxPenaltyPercent (default 25%) of invoice total

8. Utilities & Maintenance
   → Host records meter readings (electricity/water)
   → Readings auto-included in next monthly invoice
   → Tenant raises maintenance requests (plumbing, electrical, etc.)
   → Host assigns, resolves; tenant rates resolution

9. Move-Out & Settlement
   → Tenant gives notice → NOTICE_PERIOD (30 days default)
   → Auto-vacate scheduler: daily 5 AM, vacates when moveOutDate passes
   → Host initiates settlement → auto-calculates unpaid rent, utilities, penalties
   → Host adds deductions (damages, cleaning) with evidence photos
   → Inspection completed with notes
   → Dual approval: host + tenant both approve
   → Refund = security deposit - total deductions
   → If negative → tenant owes additional amount
   → Razorpay payout to tenant (via UPI or bank transfer)
   → Settlement marked SETTLED, tenancy VACATED

10. Room Occupancy Sync
    → tenancy.created Kafka → listing-service increments bed occupancy
    → tenancy.vacated Kafka → listing-service decrements + restores availability
```

---

## API Endpoints

### Tenancy Core (booking-service)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/pg-tenancies` | Create tenancy |
| GET | `/api/v1/pg-tenancies` | List (filter: listingId, status, tenantId) |
| GET | `/api/v1/pg-tenancies/{id}` | Get single |
| POST | `/api/v1/pg-tenancies/{id}/notice` | Give notice |
| POST | `/api/v1/pg-tenancies/{id}/vacate` | Vacate |
| PATCH | `/api/v1/pg-tenancies/{id}/penalty-config` | Update grace/penalty/cap |
| GET | `/api/v1/pg-tenancies/{id}/invoices` | List invoices |
| POST | `/api/v1/pg-tenancies/invoices/{id}/pay` | Mark paid |
| GET | `/api/v1/pg-tenancies/invoices/overdue` | Overdue invoices |
| GET | `/api/v1/pg-tenancies/my-dashboard` | Tenant dashboard |

### Agreement (booking-service)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `…/{id}/agreement` | Create agreement |
| GET | `…/{id}/agreement` | Get agreement |
| POST | `…/{id}/agreement/host-sign` | Host e-sign |
| POST | `…/{id}/agreement/tenant-sign` | Tenant e-sign |
| GET | `…/{id}/agreement/text` | Agreement text |
| GET | `…/{id}/agreement/view` | HTML view (public) |
| GET | `…/{id}/agreement/pdf` | Download PDF (public) |
| GET | `…/{id}/agreement/pdf/inline` | View PDF inline (public) |

### Settlement (booking-service)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `…/{id}/settlement` | Initiate |
| GET | `…/{id}/settlement` | Get details |
| POST | `…/{id}/settlement/deductions` | Add deduction |
| DELETE | `…/{id}/settlement/deductions/{did}` | Remove deduction |
| POST | `…/{id}/settlement/inspection` | Complete inspection |
| POST | `…/{id}/settlement/approve` | Host/tenant approve |
| POST | `…/{id}/settlement/process-refund` | Trigger refund (with UPI) |
| POST | `…/{id}/settlement/mark-settled` | Mark settled |

### Utility Readings (booking-service)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `…/{id}/utility-readings` | Record reading |
| GET | `…/{id}/utility-readings` | List (filter by type) |
| GET | `…/{id}/utility-readings/unbilled` | Unbilled charges |

### Maintenance Requests (booking-service)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `…/{id}/maintenance` | Create request |
| GET | `…/{id}/maintenance` | List (filter by status) |
| GET | `…/{id}/maintenance/{rid}` | Get single |
| PUT | `…/{id}/maintenance/{rid}` | Update status/assign |
| POST | `…/{id}/maintenance/{rid}/rate` | Rate resolution |

### Tenant Subscription (payment-service)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/payments/tenancy/{id}/subscription` | Create Razorpay subscription |
| GET | `/api/v1/payments/tenancy/{id}/subscription` | Get status |
| POST | `/api/v1/payments/tenancy/{id}/subscription/cancel` | Cancel |
| POST | `/api/v1/payments/tenancy/webhook/subscription` | Razorpay webhook |

### Host Payouts (payment-service)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/payments/host-payouts` | List payouts |
| GET | `/api/v1/payments/host-payouts/summary` | Monthly reconciliation |
| POST | `/api/v1/payments/host-payouts/{id}/execute` | Execute transfer |
| POST | `/api/v1/payments/host-payouts/{id}/retry` | Retry failed |

---

## Scheduled Jobs

| Job | Schedule | Service | Purpose |
|-----|----------|---------|---------|
| dailyBillingRun | 6 AM | booking | Generate monthly invoices |
| applyLatePenalties | 7 AM | booking | Penalize overdue invoices |
| autoVacateScheduler | 5 AM | booking | Auto-vacate expired notice periods |
| processPendingPayouts | 2 AM | payment | Execute pending host payouts |
| retryFailedPayouts | 3 AM | payment | Retry failed payouts (max 3) |

---

## Kafka Event Flow

```
booking-service produces:
  tenancy.created → listing-service (room occupancy++)
  tenancy.notice → notification-service
  tenancy.vacated → listing-service (room occupancy--), notification-service
  tenancy.invoice.generated → notification-service
  tenancy.invoice.overdue → notification-service
  tenancy.agreement.created → notification-service
  tenancy.agreement.host-signed → notification-service
  tenancy.agreement.active → notification-service
  tenancy.settlement.initiated → notification-service
  tenancy.settlement.approved → notification-service
  tenancy.settled → notification-service
  tenancy.rent.collected → payment-service (host payout)
  maintenance.request.created → notification-service
  maintenance.request.resolved → notification-service

payment-service produces:
  tenancy.subscription.created → booking-service
  tenancy.subscription.authenticated → booking-service
  tenancy.subscription.charged → booking-service (mark invoice PAID)
  tenancy.subscription.halted → booking-service (mark invoice OVERDUE)
  host.payout.completed → notification-service
  host.payout.failed → notification-service
  payout.completed → (RazorpayX webhook)
  payout.reversed → (RazorpayX webhook)
  payout.failed → (RazorpayX webhook)
```

---

## Flyway Migrations

### booking-service (V25–V29)
| Migration | Table/Change |
|-----------|-------------|
| V25 | tenancy_agreements |
| V26 | ALTER pg_tenancies + tenancy_invoices (subscription/penalty fields) |
| V27 | tenancy_settlements + settlement_deductions |
| V28 | utility_readings |
| V29 | maintenance_requests |

### payment-service (V7–V8)
| Migration | Table |
|-----------|-------|
| V7 | tenancy_subscriptions |
| V8 | host_payouts |

### listing-service (V51)
| Migration | Change |
|-----------|--------|
| V51 | ALTER listings (grace_period_days, late_penalty_bps) |

### user-service (V23)
| Migration | Change |
|-----------|--------|
| V23 | ALTER host_kyc (razorpay_linked_account_id) |

---

## Frontend Screens

### Web (Next.js) — Tenant
| Route | Purpose |
|-------|---------|
| `/pg-dashboard` | Tenant PG dashboard: stats, agreement, invoice, quick actions |
| `/pg-dashboard/maintenance` | Maintenance requests: create, filter, rate |
| `/pg-dashboard/utilities` | Utility readings: electricity/water, unbilled |

### Web (Next.js) — Host
| Tab | Purpose |
|-----|---------|
| Settlement | Move-out settlements: deductions, inspection, approve, refund |
| Payouts | Commission earnings: monthly summary, payout history |
| Tenancy (existing) | Tenancy management, penalty config |
| Packages (existing) | PG packages |

### Mobile (React Native Expo)
| Screen | Purpose |
|--------|---------|
| `pg-dashboard` | Tenant dashboard: stats, agreement sign, invoice, quick actions |
| `pg-maintenance` | Maintenance: categories, priority, FAB, star rating |
| `pg-invoices` | Invoice history with itemized breakdown |
| `pg-agreement` | Agreement: progress stepper, sign, PDF download |

---

## Commission Model

| Host Tier | Commission Rate | Monthly Fee | Net Example (₹10,000 rent) |
|-----------|----------------|-------------|----------------------------|
| STARTER | 18% | ₹999 | ₹10,000 - ₹1,800 - ₹324 (GST) = ₹7,876 |
| PRO | 12% | ₹2,499 | ₹10,000 - ₹1,200 - ₹216 (GST) = ₹8,584 |
| COMMERCIAL | 10% | ₹3,999 | ₹10,000 - ₹1,000 - ₹180 (GST) = ₹8,820 |
| MEDICAL | 8% | — | ₹10,000 - ₹800 - ₹144 (GST) = ₹9,056 |
| AASHRAY | 0% | — | ₹10,000 (full amount) |

*TDS of 1% applies additionally when monthly rent exceeds ₹50,000*

---

## Penalty Configuration

| Field | Default | Range | Description |
|-------|---------|-------|-------------|
| gracePeriodDays | 5 | 0–30 | Days after due date before penalty starts |
| latePenaltyBps | 200 | 0–500 | Basis points per day (200 = 2%/day) |
| maxPenaltyPercent | 25 | 0–100 | Maximum penalty cap as % of invoice total |

**Configurable at 3 levels** (priority order):
1. Per-tenancy override (PATCH endpoint)
2. Per-listing default (host sets in listing config)
3. System default (5 days / 200 bps / 25%)

---

## Settlement Formula

```
Total Deductions = Unpaid Rent + Unpaid Utilities + Damages + Late Penalties + Cleaning + Other

If Security Deposit >= Total Deductions:
  Refund Amount = Security Deposit - Total Deductions
  → Razorpay payout to tenant

If Security Deposit < Total Deductions:
  Additional Due = Total Deductions - Security Deposit
  → Tenant owes extra (final invoice generated)
```

---

## Key Entities

### PgTenancy (booking-service)
tenancyRef, tenantId, listingId, roomTypeId, bedNumber, sharingType, moveInDate, moveOutDate, noticePeriodDays, monthlyRentPaise, securityDepositPaise, status (ACTIVE/NOTICE_PERIOD/VACATED/TERMINATED), billingDay, nextBillingDate, gracePeriodDays, latePenaltyBps, maxPenaltyPercent, razorpaySubscriptionId, subscriptionStatus

### TenancyAgreement (booking-service)
agreementNumber, tenancyId, tenantName/Phone/Email, hostName/Phone, propertyAddress, moveInDate, lockInPeriodMonths, noticePeriodDays, monthlyRentPaise, securityDepositPaise, agreementText, status (DRAFT→PENDING_HOST_SIGN→PENDING_TENANT_SIGN→ACTIVE), hostSignedAt, tenantSignedAt, agreementPdfUrl

### TenancyInvoice (booking-service)
invoiceNumber, tenancyId, billingMonth/Year, rentPaise, packagesPaise, electricityPaise, waterPaise, gstPaise, latePenaltyPaise, grandTotalPaise, status (GENERATED/PAID/OVERDUE), dueDate, paidDate

### TenancySettlement (booking-service)
settlementRef, tenancyId, moveOutDate, inspectionDate/Notes, securityDepositPaise, unpaidRentPaise, unpaidUtilitiesPaise, damageDeductionPaise, latePenaltyPaise, otherDeductionsPaise, totalDeductionsPaise, refundAmountPaise, additionalDuePaise, status (INITIATED→INSPECTION_DONE→APPROVED→REFUND_PROCESSING→SETTLED), approvedByHostAt, approvedByTenantAt

### HostPayout (payment-service)
tenancyId, hostId, invoiceId, grossAmountPaise, commissionRateBps, commissionPaise, gstOnCommissionPaise, tdsAmountPaise, netPayoutPaise, payoutStatus (PENDING/PROCESSING/COMPLETED/FAILED), razorpayTransferId, payoutDate

### UtilityReading (booking-service)
tenancyId, utilityType (ELECTRICITY/WATER), readingDate, meterNumber, previousReading, currentReading, unitsConsumed, ratePerUnitPaise, totalChargePaise, invoiceId

### MaintenanceRequest (booking-service)
tenancyId, requestNumber, category (PLUMBING/ELECTRICAL/FURNITURE/APPLIANCE/CLEANING/PEST_CONTROL/OTHER), title, description, priority (LOW/MEDIUM/HIGH/URGENT), status (OPEN→ACKNOWLEDGED→IN_PROGRESS→RESOLVED→CLOSED), assignedTo, tenantRating, tenantFeedback
