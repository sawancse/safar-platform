# PRD: Auth Enhancement — Password/PIN Login Options, Security Settings & Bug Fixes

**Version:** 1.0
**Date:** 2026-04-12
**Status:** Implemented

---

## 1. Executive Summary

Enhanced the authentication system to give users choice between OTP, Password, and PIN on every login. Added security settings dashboard for managing credentials, forgot-PIN pre-auth flow, PIN complexity rules, device management, and fixed 3 critical bugs (builder price edit, land sale creation, search param mismatch).

---

## 2. Auth Enhancement

### 2.1 Login Flow — User Chooses Method

**Before:** System auto-detected one method and jumped to it.
**After:** When multiple methods are available, user sees a choice screen.

```
Enter phone/email
  → System detects ALL available methods (OTP, Password, PIN)
  → If multiple options exist → Show choice screen:
      [ Sign in with OTP      ] — Receive a code via SMS/email
      [ Sign in with Password ] — Use your account password
      [ Sign in with PIN      ] — Quick login with 4-6 digit PIN
  → If only one method → Skip directly to it (same as before)
```

**New step:** `choose-method` in the login wizard.

### 2.2 Forgot PIN (Pre-Auth)

**Before:** PIN could only be reset after logging in with OTP first.
**After:** Direct forgot-PIN flow without login.

```
PIN login screen → "Forgot PIN?"
  → OTP sent to phone
  → Enter OTP + New PIN (side by side)
  → "Reset PIN & Sign In" → auto-logs in
```

**New endpoint:** `POST /api/v1/auth/pin/forgot`
- Body: `{ phone, otp, newPin }`
- Verifies OTP, resets PIN, returns AuthResponse (auto-login)

### 2.3 PIN Complexity Rules

Blocked 30+ weak PINs:
- All same digits: 0000, 1111, 2222...9999, 000000, 111111
- Sequential: 1234, 4321, 0123, 3210, 5678, 8765, 12345, 54321, 123456, 654321
- Common patterns: 1212, 2121, 112233, 121212, 131313, 696969, 123123

Applied to: `setPin()`, `changePin()`, `resetPinAfterOtp()`, `forgotPin()`

### 2.4 Unified Auth Options Endpoint

**New endpoint:** `GET /api/v1/auth/auth-options?phone=&email=`

Returns:
```json
{
  "otp": true,
  "password": true,
  "pin": true,
  "pinLocked": false,
  "exists": true
}
```

Used by frontend to determine which login methods to show.

### 2.5 Security Settings Page

**Route:** `/settings/security`

**Sections:**
1. **Login Methods Overview** — Shows OTP (always active), Password (set/not set), PIN (set/not set)
2. **Set Password** — For users who don't have one yet (8+ chars, 1 uppercase, 1 digit, strength meter)
3. **Change Password** — Requires current password verification
4. **Set PIN** — 4-6 digits with complexity check
5. **Change PIN** — Requires current PIN
6. **Remove PIN** — Soft confirmation
7. **Security Tips** — Guidance on reducing OTP dependency

### 2.6 Device Management

**New endpoints:**
- `GET /api/v1/auth/devices` — List all trusted devices (fingerprint, name, expiry)
- `DELETE /api/v1/auth/devices/all` — Revoke all trusted devices (sign out everywhere)

**Backend:** `AuthService.listTrustedDevices()` scans Redis for `trusted:device:{userId}:*` keys.

### 2.7 Security Logging

Added `log.warn("SECURITY:")` on:
- Password set, changed, reset
- PIN set, changed, reset

(Kafka notification events deferred — auth-service doesn't have spring-kafka dependency)

---

## 3. Bug Fixes

### 3.1 Builder Project Price Edit (Bug Fix)

**Symptom:** Unable to edit price of builder project.

**Root cause:** `BuilderProjectService.updateProject()` updated 22 fields but skipped `minPricePaise`, `maxPricePaise`, `minBhk`, `maxBhk`, `minAreaSqft`, `maxAreaSqft`. The `CreateBuilderProjectRequest` DTO also lacked these fields.

**Fix:**
- Added 6 fields to `CreateBuilderProjectRequest` record
- Added 6 conditional updates in `BuilderProjectService.updateProject()`
- Prices now editable directly (in addition to auto-computation from unit types)

### 3.2 Land Sale Creation (Bug Fix)

**Symptom:** Cannot create sale listings for agricultural land, farming land, etc.

**Root cause:** Backend was fine (all enum types supported). Frontend `/sell` page had 3 issues:
1. `PROPERTY_TYPES` array missing: AGRICULTURAL_LAND, FARMING_LAND, RESIDENTIAL_PLOT, COMMERCIAL_LAND, INDUSTRIAL_LAND
2. Wrong enum values: `SHOP` instead of `COMMERCIAL_SHOP`, `SHOWROOM` instead of `COMMERCIAL_SHOWROOM`, `WAREHOUSE` instead of `COMMERCIAL_WAREHOUSE`
3. `isPlot` check only matched `'PLOT'` — new land types were treated as built properties, forcing BHK/bathrooms/floors validation

**Fix:**
- Added all 6 new land types + fixed 3 commercial enum values in PROPERTY_TYPES
- Expanded `isPlot` to array check across 3 locations (validation step 2, step 3, and rendering)
- Photos made optional for all land types

### 3.3 Search Not Working (Bug Fix)

**Symptom:** Clicking "Farm & Agriculture" or other new tabs shows all properties instead of filtering.

**Root cause:** Two mismatches between frontend and backend:
1. **Parameter name:** Frontend sent `type=` but backend `@RequestParam List<String> salePropertyType` expects `salePropertyType=`
2. **Format:** Frontend sent comma-separated string `type=A,B,C` but Spring expects repeated params `salePropertyType=A&salePropertyType=B&salePropertyType=C`

**Fix:** Changed frontend to send `params.salePropertyType = ['AGRICULTURAL_LAND', 'FARMING_LAND', 'FARM_HOUSE']` as array. The existing `searchSaleProperties` API function already handles arrays via `URLSearchParams.append()`.

---

## 4. Other Fixes in This Session

### 4.1 Messaging Service Connection Leak

**Symptom:** `Apparent connection leak detected` on startup.

**Root cause:** 
- HikariCP pool size was 2 (too small — Flyway + JPA validation exhaust pool)
- Kafka send inside `@Transactional` held DB connection during broker timeout
- Leak detection threshold 15s too aggressive for startup

**Fix:**
- Pool size 2 → 5, leak threshold 15s → 60s
- Moved Kafka `publishMessageCreatedEvent()` to `TransactionSynchronization.afterCommit()` — DB connection released before Kafka send

### 4.2 Chef Service RestTemplate Missing

**Symptom:** `Parameter 2 of constructor required a bean of type RestTemplate that could not be found`

**Fix:** Created `chef-service/config/AppConfig.java` with `@Bean RestTemplate`

### 4.3 User Service AES Key Missing

**Symptom:** `AES256Encryptor Constructor threw exception` — empty default for `encryption.aes-key`

**Fix:** Set default to the existing key from `.env`: `t+YZDpLpyNyfaZcSbH9Ak2CMYbJjBKzEdJYZR130CDI=`

### 4.4 Admin Professionals Import Path

**Symptom:** `Failed to resolve import "../services/api"`

**Fix:** Changed to `import { adminApi as api } from '../lib/api'` (correct path)

---

## 5. Files Modified

### auth-service
| File | Change |
|------|--------|
| `service/PinService.java` | PIN complexity validation, `forgotPin()` method, OtpService injection |
| `service/PasswordService.java` | Security logging on set/change/reset |
| `service/AuthService.java` | `listTrustedDevices()`, `revokeAllDevices()` |
| `controller/AuthController.java` | `POST /pin/forgot`, `GET /auth-options`, `GET /devices`, `DELETE /devices/all` |

### listing-service
| File | Change |
|------|--------|
| `dto/CreateBuilderProjectRequest.java` | +6 price/config fields |
| `service/BuilderProjectService.java` | Price fields in updateProject() |

### messaging-service
| File | Change |
|------|--------|
| `application.yml` | Pool 2→5, leak threshold 15s→60s |
| `service/MessagingService.java` | Kafka send moved to afterCommit() |

### chef-service
| File | Change |
|------|--------|
| `config/AppConfig.java` | NEW — RestTemplate bean |

### user-service
| File | Change |
|------|--------|
| `application.yml` | AES key default from .env |

### safar-web
| File | Change |
|------|--------|
| `app/auth/page.tsx` | `choose-method` step, `forgot-pin` step, enhanced auth detection |
| `app/settings/security/page.tsx` | NEW — Security settings with password/PIN management |
| `app/buy/page.tsx` | Fixed `type` → `salePropertyType` as array |
| `app/sell/page.tsx` | Added land types, fixed enum values, expanded isPlot |
| `lib/api.ts` | `getAuthOptions()`, `forgotPin()` |

### admin
| File | Change |
|------|--------|
| `pages/ProfessionalsPage.tsx` | Fixed import path |
