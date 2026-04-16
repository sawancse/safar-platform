# PRD: PG Recurring Billing & Rent Collection

## Overview
Automated monthly rent billing system for PG (Paying Guest), Hostel, and Co-living tenancies on the Safar platform. Handles invoice generation, Razorpay subscription auto-debit, multi-channel notifications (Email + SMS + In-App), late payment penalties, and host payouts.

## Status: **Fully Implemented**

---

## 1. Core Entities

### 1.1 PG Tenancy (`bookings.pg_tenancies`)
Stores the tenancy lifecycle and billing configuration.

| Field | Type | Description |
|-------|------|-------------|
| `tenancy_ref` | VARCHAR(20) | Unique ref (PGT-YYYY-NNNN) |
| `tenant_id` | UUID | Links to auth-service user |
| `listing_id` | UUID | The PG property |
| `room_type_id` | UUID | Assigned room type |
| `bed_number` | VARCHAR(10) | Assigned bed |
| `sharing_type` | VARCHAR(30) | PRIVATE, TWO_SHARING, THREE_SHARING, FOUR_SHARING, DORMITORY |
| `monthly_rent_paise` | BIGINT | Base rent in paise |
| `total_monthly_paise` | BIGINT | Rent + packages (meals, laundry, WiFi) |
| `security_deposit_paise` | BIGINT | Refundable deposit |
| `billing_day` | INT | Day of month to bill (default 1) |
| `next_billing_date` | DATE | When next invoice will be generated |
| `grace_period_days` | INT | Days after due before penalty (default 5) |
| `late_penalty_bps` | INT | Daily penalty rate in basis points (default 200 = 2%/day) |
| `max_penalty_percent` | INT | Penalty cap as % of invoice total (default 25%) |
| `razorpay_subscription_id` | VARCHAR | Razorpay subscription ID for auto-debit |
| `razorpay_plan_id` | VARCHAR | Razorpay plan ID |
| `subscription_status` | VARCHAR | CREATED, AUTHENTICATED, ACTIVE, HALTED, CANCELLED |
| `rent_advance_reminder_sent` | BOOLEAN | Flag for 7-day advance reminder (reset each cycle) |
| `status` | ENUM | ACTIVE, NOTICE_PERIOD, VACATED, PENDING, TERMINATED |

### 1.2 Tenancy Invoice (`bookings.tenancy_invoices`)
Monthly billing records with line-item breakdown.

| Field | Type | Description |
|-------|------|-------------|
| `invoice_number` | VARCHAR(30) | Unique (INV-PG-YYYY-NNNN) |
| `tenancy_id` | UUID | Parent tenancy |
| `tenant_id` | UUID | Tenant user ID (for Kafka events) |
| `billing_month` / `billing_year` | INT | Billing period |
| `rent_paise` | BIGINT | Base rent |
| `packages_paise` | BIGINT | Meals, laundry, WiFi |
| `electricity_paise` | BIGINT | From utility meter readings |
| `water_paise` | BIGINT | From utility meter readings |
| `total_paise` | BIGINT | Sum before GST |
| `gst_paise` | BIGINT | 18% GST |
| `grand_total_paise` | BIGINT | Final amount due |
| `late_penalty_paise` | BIGINT | Accumulated late charge |
| `due_date` | DATE | billing_date + 7 days |
| `paid_date` | DATE | When actually paid |
| `razorpay_payment_id` | VARCHAR | Razorpay payment ref |
| `status` | ENUM | GENERATED, SENT, PAID, OVERDUE, WAIVED, PARTIAL |
| `reminder_5d_sent` | BOOLEAN | 5-day reminder sent flag |
| `reminder_1d_sent` | BOOLEAN | 1-day urgent reminder sent flag |

### 1.3 Tenancy Subscription (`payments.tenancy_subscriptions`)
Razorpay subscription state tracking.

| Field | Type | Description |
|-------|------|-------------|
| `tenancy_id` | UUID | Parent tenancy |
| `tenant_id` | UUID | Tenant user ID |
| `razorpay_plan_id` | VARCHAR | Razorpay plan |
| `razorpay_subscription_id` | VARCHAR | Razorpay subscription |
| `amount_paise` | BIGINT | Monthly charge amount |
| `status` | VARCHAR | CREATED, AUTHENTICATED, ACTIVE, PENDING, HALTED, CANCELLED, COMPLETED |
| `charge_attempts` | INT | Failure retry count |
| `last_charged_at` | TIMESTAMPTZ | Last successful charge |
| `failure_reason` | VARCHAR | Error on failure |

### 1.4 Host Payout (`payments.host_payouts`)
Settlement after commission deduction.

| Field | Type | Description |
|-------|------|-------------|
| `gross_amount_paise` | BIGINT | Invoice grand total |
| `commission_rate_bps` | INT | Host tier rate (1800/1200/1000/800/0) |
| `commission_paise` | BIGINT | Platform commission |
| `gst_on_commission_paise` | BIGINT | 18% GST on commission |
| `tds_amount_paise` | BIGINT | 1% TDS |
| `net_payout_paise` | BIGINT | Final transfer to host |
| `payout_status` | VARCHAR | PENDING, SCHEDULED, PROCESSING, COMPLETED, FAILED |

---

## 2. Billing Flow

### 2.1 Monthly Cycle Timeline

```
Day -7 (8 AM)   → sendAdvanceRentReminders()
                   Find ACTIVE tenancies with nextBillingDate = today+7
                   Publish tenancy.rent.reminder.advance → Email + SMS + In-App
                   Set rentAdvanceReminderSent = true

Day 0 (6 AM)    → dailyBillingRun() → generateMonthlyInvoices()
                   For each tenancy with nextBillingDate = today:
                     1. Collect: rent + packages + electricity + water
                     2. Calculate 18% GST
                     3. Create TenancyInvoice (status=GENERATED, dueDate=+7)
                     4. Advance nextBillingDate += 1 month
                     5. Reset rentAdvanceReminderSent = false
                     6. Publish tenancy.invoice.generated → Email + SMS + In-App

Day 2 (9 AM)    → sendPreDueReminders()
                   Find invoices with dueDate = today+5 AND reminder_5d_sent=false
                   Publish tenancy.rent.reminder → Email + SMS + In-App
                   Set reminder_5d_sent = true

Day 6 (9:30 AM) → sendUrgentReminders()
                   Find invoices with dueDate = today+1 AND reminder_1d_sent=false
                   Publish tenancy.rent.reminder.urgent → Email + SMS + In-App
                   Set reminder_1d_sent = true

Day 7           → Razorpay auto-debit (subscription.charged webhook)
                   → TenancyPaymentService marks subscription ACTIVE
                   → Kafka tenancy.subscription.charged
                   → TenancyPaymentListener marks invoice PAID
                   → Kafka tenancy.rent.collected
                   → RentCollectedListener calculates host payout

Day 7+ (7 AM)   → applyLatePenalties()
                   For unpaid invoices past dueDate + gracePeriodDays:
                     penalty = grandTotal × latePenaltyBps × daysOverdue / 10000
                     Cap at maxPenaltyPercent
                     Mark OVERDUE
                     Publish tenancy.invoice.overdue → Email + SMS + In-App
```

### 2.2 Razorpay Subscription Lifecycle

```
1. POST /api/v1/payments/tenancy/{tenancyId}/subscription
   → Creates Razorpay plan (monthly, interval=1)
   → Creates subscription (total_count=120 = 10 years)
   → Status: CREATED

2. Tenant authenticates (card/UPI details)
   → Webhook: subscription.authenticated → AUTHENTICATED

3. Monthly billing cycle
   → Razorpay auto-debits
   → Success: subscription.charged → ACTIVE, invoice PAID
   → 3 retries on failure
   → All fail: subscription.halted → HALTED, invoice OVERDUE

4. Cancel: POST /api/v1/payments/tenancy/{tenancyId}/subscription/cancel
   → Cancels at end of billing cycle
```

---

## 3. Notification System

### 3.1 Channels
All rent events send notifications via **3 channels**:
- **Email** — HTML templates via Thymeleaf + Spring Mail
- **SMS** — MSG91 Flow API (transactional SMS, DLT-registered templates)
- **In-App** — `InAppNotificationService` stored in notifications schema

### 3.2 Kafka Topics

| Topic | Producer | Consumer | Trigger |
|-------|----------|----------|---------|
| `tenancy.rent.reminder.advance` | booking-service | notification-service | 7 days before billing |
| `tenancy.invoice.generated` | booking-service | notification-service | Invoice created |
| `tenancy.rent.reminder` | booking-service | notification-service | 5 days before due |
| `tenancy.rent.reminder.urgent` | booking-service | notification-service | 1 day before due |
| `tenancy.invoice.overdue` | booking-service | notification-service | Past due + grace period |
| `tenancy.subscription.charged` | payment-service | booking-service | Razorpay payment success |
| `tenancy.subscription.halted` | payment-service | booking-service | Razorpay retries exhausted |
| `tenancy.rent.collected` | booking-service | payment-service | Invoice marked PAID |

### 3.3 Email Templates

| Template | Subject | When |
|----------|---------|------|
| `tenancy-rent-reminder-advance` | "Rent Reminder - Payment Due in 7 Days" | Day -7 |
| `tenancy-invoice` | "Rent Invoice INV-PG-YYYY-NNNN - Month Year" | Day 0 |
| `tenancy-rent-reminder` | "Rent Due in 5 Days - INV-PG-..." | Day 2 |
| `tenancy-rent-reminder-urgent` | "Rent Due Tomorrow - INV-PG-..." | Day 6 |
| `tenancy-invoice-overdue` | "Overdue: Rent Payment Required - INV-PG-..." | Day 7+ |

### 3.4 SMS Templates (MSG91 DLT)
Configured via env vars. Gracefully skipped if template ID not set.

| Env Var | Purpose |
|---------|---------|
| `MSG91_SMS_RENT_ADVANCE_TEMPLATE` | 7-day advance reminder |
| `MSG91_SMS_RENT_REMINDER_TEMPLATE` | 5-day reminder |
| `MSG91_SMS_RENT_URGENT_TEMPLATE` | 1-day urgent |
| `MSG91_SMS_RENT_OVERDUE_TEMPLATE` | Overdue notice |
| `MSG91_SMS_INVOICE_TEMPLATE` | New invoice |

---

## 4. Penalty Configuration

Per-tenancy configurable penalty system:

| Field | Default | Description |
|-------|---------|-------------|
| `grace_period_days` | 5 | Days after due date before penalties apply |
| `late_penalty_bps` | 200 | Daily penalty rate (200 bps = 2%/day) |
| `max_penalty_percent` | 25 | Maximum penalty cap (25% of invoice total) |

**Example:**
- Invoice: ₹10,000 | Due: April 1 | Grace: 5 days
- Payment on April 8 (2 days overdue):
  - Penalty = ₹10,000 × 2% × 2 = ₹400
  - If cap hit: min(₹400, ₹2,500) = ₹400
  - Total due: ₹10,400

---

## 5. Host Payout Formula

After rent collection, host payout is calculated:

```
commissionPaise    = grandTotalPaise × commissionRateBps / 10000
gstOnCommission    = commissionPaise × 18 / 100
tdsAmount          = (grandTotalPaise - commissionPaise) × 1 / 100
netPayoutPaise     = grandTotalPaise - commissionPaise - gstOnCommission - tdsAmount
```

Commission rates by host subscription tier:
| Tier | Rate |
|------|------|
| STARTER | 18% (1800 bps) |
| PRO | 12% (1200 bps) |
| COMMERCIAL | 10% (1000 bps) |
| MEDICAL | 8% (800 bps) |
| AASHRAY | 0% |

---

## 6. Tenant-Facing UI

### 6.1 PG Dashboard (`/pg-dashboard`)
- Overview: tenancy status, monthly rent, outstanding dues, total paid, security deposit
- Current invoice card with Pay Now button
- Auto-debit subscription status (ACTIVE/NOT_SET_UP)
- Agreement review/sign/download
- Give notice button

### 6.2 Invoices (`/pg-dashboard/invoices`)
- Paginated invoice list with status badges
- Line-item breakdown: rent, packages, electricity, water, GST
- Late penalty display
- Pay Now button for unpaid invoices

### 6.3 Navigation
- Navbar: "PG / Rent" link in traveller menu → `/pg-dashboard`

---

## 7. API Endpoints

### Tenant
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/pg-tenancies/my-dashboard` | Tenant dashboard summary |
| GET | `/api/v1/pg-tenancies/{id}/invoices` | Invoice list |
| POST | `/api/v1/pg-tenancies/{id}/give-notice` | Start notice period |
| POST | `/api/v1/pg-tenancies/{id}/agreement/sign` | Tenant signs agreement |
| GET | `/api/v1/pg-tenancies/{id}/agreement/view` | View agreement HTML |
| GET | `/api/v1/pg-tenancies/{id}/agreement/pdf` | Download agreement PDF |

### Payment
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/payments/tenancy/{id}/subscription` | Create Razorpay subscription |
| GET | `/api/v1/payments/tenancy/{id}/subscription` | Get subscription status |
| POST | `/api/v1/payments/tenancy/{id}/subscription/cancel` | Cancel subscription |
| POST | `/api/v1/payments/tenancy/webhook/subscription` | Razorpay webhook handler |

### Admin
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/pg/settlements` | All settlements |
| POST | `/api/v1/admin/pg/settlements/{id}/override` | Override payment decision |
| GET | `/api/v1/admin/pg/settlements/stats` | Settlement analytics |

---

## 8. Schedulers

| Cron | Time | Method | Service |
|------|------|--------|---------|
| `0 0 5 * * *` | 5 AM | `autoVacateExpiredTenancies()` | booking-service |
| `0 0 6 * * *` | 6 AM | `dailyBillingRun()` | booking-service |
| `0 0 7 * * *` | 7 AM | `applyLatePenalties()` | booking-service |
| `0 0 8 * * *` | 8 AM | `sendAdvanceRentReminders()` | booking-service |
| `0 0 9 * * *` | 9 AM | `sendPreDueReminders()` | booking-service |
| `0 30 9 * * *` | 9:30 AM | `sendUrgentReminders()` | booking-service |

---

## 9. Flyway Migrations

| Service | Migration | Description |
|---------|-----------|-------------|
| booking-service | V19 | pg_tenancies + tenancy_invoices tables |
| booking-service | V26 | Subscription fields on tenancy |
| booking-service | V28 | Utility readings |
| booking-service | V30 | Configurable penalty |
| booking-service | V39 | Lease duration + reminder flags |
| booking-service | V40 | Invoice tenant_id + backfill |
| booking-service | V41 | rent_advance_reminder_sent flag |
| booking-service | V42 | Agreement signing audit (hostSignedBy, tenantSignedBy, version) |
| payment-service | V7 | tenancy_subscriptions table |

---

## 10. Key Files

| Component | Path |
|-----------|------|
| Invoice Entity | `booking-service/.../entity/TenancyInvoice.java` |
| Tenancy Entity | `booking-service/.../entity/PgTenancy.java` |
| Invoice Generation | `booking-service/.../service/PgTenancyService.java` |
| Billing Scheduler | `booking-service/.../service/TenancyBillingScheduler.java` |
| Subscription Service | `payment-service/.../service/TenancyPaymentService.java` |
| Razorpay Gateway | `payment-service/.../gateway/RazorpayPaymentGateway.java` |
| Payment Listener | `booking-service/.../kafka/TenancyPaymentListener.java` |
| Rent Payout Listener | `payment-service/.../kafka/RentCollectedListener.java` |
| Notification Consumer | `notification-service/.../kafka/TenancyEventConsumer.java` |
| SMS Service | `notification-service/.../service/SmsService.java` |
| Agreement Entity | `booking-service/.../entity/TenancyAgreement.java` |
| Agreement Service | `booking-service/.../service/TenancyAgreementService.java` |
| Agreement Controller | `booking-service/.../controller/TenancyAgreementController.java` |
| Agreement PDF | `booking-service/.../service/AgreementPdfService.java` |
| Tenant Dashboard | `safar-web/app/pg-dashboard/page.tsx` |
| Invoice Page | `safar-web/app/pg-dashboard/invoices/page.tsx` |

---

## 11. Agreement Signing Flow

### 11.1 Agreement Entity (`bookings.tenancy_agreements`)

| Field | Type | Description |
|-------|------|-------------|
| `tenancy_id` | UUID | Parent tenancy |
| `agreement_number` | VARCHAR(30) | Unique (AGR-YYYY-NNNN) |
| `tenant_name` | VARCHAR(200) | Tenant full name |
| `tenant_phone` | VARCHAR(20) | Tenant phone |
| `tenant_email` | VARCHAR(200) | Tenant email |
| `tenant_aadhaar_last4` | VARCHAR(4) | Aadhaar last 4 digits |
| `host_name` | VARCHAR(200) | Host/licensor name |
| `host_phone` | VARCHAR(20) | Host phone |
| `property_address` | TEXT | Full property address |
| `room_description` | VARCHAR(500) | Room/bed assignment |
| `move_in_date` | DATE | Tenancy start date |
| `lock_in_period_months` | INT | Lock-in duration |
| `notice_period_days` | INT | Notice period |
| `monthly_rent_paise` | BIGINT | Monthly rent |
| `security_deposit_paise` | BIGINT | Security deposit |
| `maintenance_charges_paise` | BIGINT | Maintenance charges |
| `agreement_text` | TEXT | Full agreement text (auto-generated) |
| `terms_and_conditions` | TEXT | Custom T&C from host |
| `status` | ENUM | DRAFT, PENDING_HOST_SIGN, PENDING_TENANT_SIGN, ACTIVE, EXPIRED, TERMINATED |
| `host_signed_at` | TIMESTAMPTZ | When host signed |
| `host_signature_ip` | VARCHAR(45) | Host's IP at signing |
| `host_signed_by` | UUID | Host's userId (audit trail) |
| `tenant_signed_at` | TIMESTAMPTZ | When tenant signed |
| `tenant_signature_ip` | VARCHAR(45) | Tenant's IP at signing |
| `tenant_signed_by` | UUID | Tenant's userId (audit trail) |
| `version` | BIGINT | Optimistic locking |
| `stamp_duty_paise` | BIGINT | E-stamp duty amount |
| `agreement_pdf_url` | VARCHAR(500) | Stored PDF URL |

### 11.2 Status Flow

```
Agreement Created → PENDING_HOST_SIGN
     ↓ (host signs — verified via X-User-Id, must NOT be tenant)
PENDING_TENANT_SIGN
     ↓ (tenant signs — verified via X-User-Id, must match tenancy.tenantId)
ACTIVE
     ↓ (expiry or termination)
EXPIRED / TERMINATED
```

### 11.3 Security Model

**Identity Verification (added 2026-04-16):**
- `POST /agreement/host-sign` — extracts `X-User-Id`, rejects if `userId == tenancy.tenantId` (tenant cannot sign as host)
- `POST /agreement/tenant-sign` — extracts `X-User-Id`, rejects if `userId != tenancy.tenantId` (only the actual tenant can sign)
- Optimistic locking (`@Version`) prevents race conditions on concurrent sign attempts

**Access Control:**
- Agreement view/pdf/text endpoints require JWT (NOT public — contains PII: name, Aadhaar, address, rent)
- Frontend uses authenticated `fetch()` calls with Bearer token, opens HTML in new window / downloads PDF as blob
- `POST /agreement` (create) requires auth (admin/host)

### 11.4 Kafka Events

| Topic | Trigger | Payload includes |
|-------|---------|-----------------|
| `tenancy.agreement.created` | Agreement created | tenantId, tenantEmail, propertyAddress, monthlyRentPaise |
| `tenancy.agreement.host-signed` | Host signs | tenantId, tenantEmail, agreementNumber, hostSignedAt |
| `tenancy.agreement.active` | Tenant signs | tenantId, tenantEmail, agreementNumber, tenantSignedAt |

All events are enriched maps (not raw entity) to ensure `tenantId` is always present for notification-service.

### 11.5 Notifications on Agreement Events

| Event | Email | In-App |
|-------|-------|--------|
| Host signs (PENDING_TENANT_SIGN) | "Your Rental Agreement is Ready - Please Review & Sign" | "Agreement Ready" |
| Tenant signs (ACTIVE) | "Rental Agreement Signed Successfully" | "Agreement Active" |

### 11.6 Agreement Text Generation

Auto-generated 10-clause legal agreement covering:
1. Premises description (room, bed, sharing type)
2. Duration (move-in, lock-in, notice period)
3. Rent (amount, billing day, grace period, late penalty rate + cap)
4. Security deposit (refund terms)
5. Inclusions (meals, laundry, WiFi)
6. House rules (gate time, no subletting, noise, guests)
7. Termination (notice period, lock-in forfeiture)
8. Settlement on vacating (utility charges, damages, deposit refund)
9. Jurisdiction (Indian courts, property city)
10. Digital signatures (IT Act 2000 compliance)

### 11.7 API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/pg-tenancies/{id}/agreement` | JWT | Create agreement |
| GET | `/api/v1/pg-tenancies/{id}/agreement` | JWT | Get agreement details |
| POST | `/api/v1/pg-tenancies/{id}/agreement/host-sign` | JWT + X-User-Id | Host signs (identity verified) |
| POST | `/api/v1/pg-tenancies/{id}/agreement/tenant-sign` | JWT + X-User-Id | Tenant signs (identity verified) |
| GET | `/api/v1/pg-tenancies/{id}/agreement/view` | JWT | View as HTML |
| GET | `/api/v1/pg-tenancies/{id}/agreement/pdf` | JWT | Download PDF |
| GET | `/api/v1/pg-tenancies/{id}/agreement/pdf/inline` | JWT | View PDF inline |
| GET | `/api/v1/pg-tenancies/{id}/agreement/text` | JWT | Plain text |
