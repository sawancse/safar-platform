# Design: Host Payout After Settlement (RazorpayX)

Status: draft · Date: 2026-11-05 · Service: payment-service (+ small user-service addition)

## 1. Problem

Admin "Settle" (`SettlementService.processSettlement`) only writes ledger entries
(`escrow_account → host_payable / platform_payable / tax_authority_payable`).
No money moves and no `payments.payouts` row is created. Example: SAF-BKG-3VPM4L has
₹2,460 in `host_payable` and zero payouts.

## 2. What exists today (verified in code)

| Piece | State |
|---|---|
| `Payout` entity / `payments.payouts` (booking-level, UPI/NEFT/IMPS) | exists, 0 rows |
| `BookingPaymentService.initiateHostPayout` / `retryPayout` | exists, called only by `POST /payments/payout` |
| Webhook `payout.processed/reversed/failed` → status + Kafka | exists, matches on `razorpay_payout_id` |
| `RazorpayGatewayImpl.createPayout` | **stub** — builds JSON, makes no HTTP call, returns fake id `pout_pending_<ref>` |
| `HostPayout` / `host_payouts` (PG rent, TDS, commission) | separate flow; `executeTransfer` **fakes success** (`txfr_<random>` → COMPLETED) |
| Host bank details | user-service `HostKyc`: `bankAccountName/Number/Ifsc`, `razorpayLinkedAccountId`; UPI only in `PaymentMethod` |

## 3. Defects to fix as part of this work

1. **Stub gateway marks payouts PROCESSING with a fake id** → webhook never matches, payout sits forever.
2. **`payouts_status_check` excludes `REVERSED`** (V1) but webhook and `retryPayout` set it → `payout.reversed` throws on save. Needs a migration.
3. **No idempotency**: nothing prevents two payouts for one booking/settlement line. `reference_id` uses only 8 chars of the booking id (collision risk); RazorpayX caps it at 40 chars.
4. **`HostPayoutService.executeTransfer` simulates success** — must not run against live keys. Gate it behind a flag or route it through the same gateway (out of scope to redesign, but disable or fix before go-live).
5. **UPI-only**: hosts submit bank account + IFSC in KYC, not UPI. Need NEFT/IMPS (`fund_account_type = bank_account`).

## 4. Design

### 4.1 Flow

```
Admin Settle ──► processSettlement (ledger) ──► HOST line COMPLETED
                                                   │ (same tx, after commit)
                                                   ▼
                              PayoutService.createForSettlementLine(line)
                                 • idempotent on settlement_line_id (UNIQUE)
                                 • status PENDING, method from host KYC
                                                   │
                      payouts.auto-execute=true ───┤──► execute now
                      false (default until live) ──┘──► wait for admin "Pay host"
                                                   ▼
                       RazorpayXClient.createPayout (HTTP, idempotency key)
                                                   ▼
                             PROCESSING + real razorpay_payout_id
                                                   ▼
                   webhook payout.processed → COMPLETED (+UTR)
                   webhook payout.failed/reversed → FAILED/REVERSED → retry
```

Settlement and transfer are deliberately separate steps: ledger posting is reversible,
a bank transfer is not. Default is **manual approval** (admin clicks "Pay host") until
RazorpayX is proven in live mode; flip `payouts.auto-execute=true` afterwards.

### 4.2 Data model (migration `V10__payout_settlement_link.sql`)

```sql
ALTER TABLE payments.payouts ADD COLUMN settlement_line_id UUID;
ALTER TABLE payments.payouts ADD COLUMN fund_account_id VARCHAR(40);
ALTER TABLE payments.payouts ADD COLUMN utr VARCHAR(40);
ALTER TABLE payments.payouts ADD COLUMN idempotency_key VARCHAR(40);
CREATE UNIQUE INDEX uq_payouts_settlement_line ON payments.payouts(settlement_line_id)
  WHERE settlement_line_id IS NOT NULL;
ALTER TABLE payments.payouts DROP CONSTRAINT payouts_status_check;
ALTER TABLE payments.payouts ADD CONSTRAINT payouts_status_check
  CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED','REVERSED','CANCELLED'));
```

Never edit V1; add V10. Backfill SAF-BKG-3VPM4L after deploy via the admin endpoint (no SQL).

### 4.3 RazorpayX client (replace stub)

- Plain `RestTemplate`/`HttpClient` to `https://api.razorpay.com/v1` with Basic auth (same key pair; RazorpayX uses the same credentials plus `RAZORPAY_ACCOUNT_NUMBER`). razorpay-java has no payouts API.
- Steps (each idempotent, results cached on the host): `POST /contacts` → `POST /fund_accounts` (`vpa` or `bank_account`) → `POST /payouts` with header `X-Payout-Idempotency: <payout.id>`.
- `reference_id` = payout UUID without dashes truncated to 40.
- `mode`: `UPI` for vpa; `IMPS` for bank ≤ ₹5,00,000 else `NEFT`.
- `queue_if_low_balance=false`: insufficient balance → FAILED with reason, not silent queueing.
- Fail closed: if `RAZORPAY_ACCOUNT_NUMBER` is blank, throw — never return a fake id.

### 4.4 Beneficiary details

- payment-service fetches `GET user-service /internal/hosts/{id}/payout-details` (internal, not public KYC): `{method, upiId | accountNumber+ifsc+name, kycVerified}`.
- Reject unless KYC verified. Full account number is read once at execution, never stored in payment-service (only `fund_account_id`).
- Prefer bank account when both exist (name-matched by penny-drop at KYC); UPI as fallback.

### 4.5 Amounts

Payout amount = settlement HOST line (`246000` for SAF-BKG-3VPM4L). Platform fee and GST lines never pay out. TDS: not applied to short-stay bookings; the PG-rent flow keeps its own 194-IB TDS in `HostPayoutService`. Reject payout if `amount ≤ 0`.

### 4.6 API

| Endpoint | Purpose |
|---|---|
| `POST /admin/payouts/by-settlement/{lineId}/execute` | create-if-missing + execute (idempotent) |
| `POST /admin/payouts/{id}/retry` | existing; allow FAILED/REVERSED, cap retries at 3 |
| `GET  /admin/payouts?bookingId=` | admin visibility |
| Admin UI | "Pay host" button next to Settle on COMPLETED bookings; show payout status + UTR |

### 4.7 Webhook hardening

- Handle `payout.processed` (store `utr`), `payout.failed`, `payout.reversed`, `payout.rejected`, `payout.queued`.
- Update only on a legal transition (PROCESSING→COMPLETED/FAILED/REVERSED); ignore duplicates.
- Signature check already fixed in `32d946a`.

### 4.8 Ledger

On payout COMPLETED: `host_payable → bank_cash` entry (amount = payout). On REVERSED: opposite entry. This closes the loop so `host_payable` balance = money still owed.

## 5. Rollout

1. Migration V10 + RazorpayX client + fail-closed behaviour (no behavioural change while `auto-execute=false`).
2. Activate RazorpayX in the dashboard; set `RAZORPAY_ACCOUNT_NUMBER`; fund the account.
3. Test mode: create payout for a test host, confirm `payout.processed` webhook lands (`api.bhramankaro.com`, enable payout events on the webhook).
4. Live: pay SAF-BKG-3VPM4L's ₹2,460 manually via the new button as the first real transfer.
5. After a week of clean payouts, enable `auto-execute`.

## 6. Open questions

- Payout timing: immediately on settle, or T+N days after checkout (dispute window)? Default proposed: admin-triggered.
- Does Safar hold funds as RazorpayX balance top-ups, or settle from the Razorpay PG balance? RazorpayX balance must be funded separately from PG settlements.
- Is host UPI collected anywhere? Today only bank details are in KYC.
- Fix or disable `HostPayoutService.executeTransfer` simulation before live PG rent runs.

## 7. Test plan

- Unit: idempotency (double execute → one payout), status-transition guard, `REVERSED` persists.
- Gateway: fail closed without account number; mocked HTTP for contact/fund-account/payout.
- Webhook: bad signature 401, duplicate event no-op, failed→retry→processed.
- Manual: ₹1 test payout in live mode before the first real one.
