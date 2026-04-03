# Razorpay Route — Auto Host Payouts

## Overview
Implement Airbnb-style automatic host payouts using Razorpay Route (Split Payments).

## How It Works
```
Guest pays ₹2,000/night
    ├── Platform commission (tier-based) → Safar's account
    └── Host payout (remaining) → Host's linked bank account
        └── Released 24hrs after check-in
```

## Commission Split by Tier
| Tier | Commission | Host Gets | Monthly Fee |
|------|-----------|-----------|-------------|
| STARTER | 18% | 82% | ₹999 |
| PRO | 12% | 88% | ₹2,499 |
| COMMERCIAL | 10% | 90% | ₹3,999 |
| MEDICAL | 8% | 92% | — |
| AASHRAY | 0% | 100% | — |

## Implementation Steps

### Phase 1: Razorpay Route Activation
- Apply for Razorpay Route on Razorpay Dashboard
- Enable "Linked Accounts" feature
- Get Route API access

### Phase 2: Host Onboarding (Linked Account)
- During KYC, collect host's bank details (account number, IFSC, PAN)
- Create Razorpay Linked Account via API: `POST /v1/accounts`
- Store `linked_account_id` in user-service (host profile)
- Host verification by Razorpay (KYC + bank validation)

### Phase 3: Payment Split on Booking
- On `payment.captured` webhook:
  1. Calculate commission based on host's subscription tier
  2. Create Razorpay Transfer: `POST /v1/payments/{id}/transfers`
     - `amount`: host's share (total - commission)
     - `account`: host's `linked_account_id`
     - `on_hold`: true (escrow until check-in + 24hrs)
     - `on_hold_until`: check-in timestamp + 24hrs
  3. Razorpay auto-releases to host's bank after hold period

### Phase 4: Payout Schedule
- **Standard**: Release 24hrs after check-in (Airbnb model)
- **Instant payout**: Host can request early release (small fee, like Airbnb)
- **Monthly stays**: Release monthly on rent due date
- **Cancellation**: Reverse transfer if cancelled before check-in

### Phase 5: Refund Handling
- **Before check-in**: Full refund to guest, reverse host transfer
- **After check-in**: Partial refund per cancellation policy
- **Non-refundable**: No refund, host keeps full amount

## API Changes Needed

### payment-service
- `RazorpayRouteService` — create linked accounts, transfers
- `SettlementService` — update to use Route instead of manual payouts
- Webhook handler for `transfer.processed`, `transfer.failed` events

### user-service
- Add `razorpayLinkedAccountId` to host profile
- KYC flow collects bank details → creates linked account

### booking-service
- On booking confirmation → trigger transfer with hold
- On check-in event → release hold (or let Razorpay auto-release)
- On cancellation → reverse transfer

## Razorpay Route API Reference
- Create Linked Account: `POST https://api.razorpay.com/v1/accounts`
- Create Transfer: `POST https://api.razorpay.com/v1/payments/{id}/transfers`
- Fetch Transfers: `GET https://api.razorpay.com/v1/transfers`
- Reverse Transfer: `POST https://api.razorpay.com/v1/transfers/{id}/reversals`

## RBI Compliance (handled by Razorpay)
- Escrow regulations for marketplace payments
- TDS deduction on host payouts (if applicable)
- GST on platform commission
- Settlement within T+2 business days

## Comparison with Current System
| Feature | Current | With Route |
|---------|---------|------------|
| Payment collection | Razorpay → Safar account | Same |
| Host payout | Manual (admin triggers) | Auto (24hrs after check-in) |
| Commission split | Calculated, not enforced | Enforced at payment level |
| Refunds | Manual | Auto-reverse transfers |
| Host onboarding | KYC only | KYC + bank linked account |
| Compliance | Manual tracking | Razorpay handles |
