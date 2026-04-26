# Services-Leg E2E Test Guide

End-to-end smoke test of the Sprint 1 + Sprint 2 slice: vendor self-onboards a cake bakery, admin approves, listing goes live.

**Prerequisites**

- Local Postgres on `:5432` with database `safar`, user `safar`, password `safar` (matches `application.yml`).
- Local Redis on `:6379` (auth-service refresh tokens).
- Local Kafka on `:9092` (chef-service publishes outbox events — non-blocking; service starts even if Kafka is down).
- Java 17, Node 20, Maven 3.9+.

```bash
docker-compose up -d   # brings up postgres + redis + kafka + zookeeper + elasticsearch
```

## Step 1 — Start backend services

```bash
# Terminal 1 — auth-service (issues JWT)
mvn -pl services/auth-service spring-boot:run

# Terminal 2 — user-service (vendor profile)
mvn -pl services/user-service spring-boot:run

# Terminal 3 — chef-service (will be services-service after rename)
mvn -pl services/chef-service spring-boot:run

# Terminal 4 — api-gateway (routes /api/v1/services/** -> chef-service)
mvn -pl services/api-gateway spring-boot:run
```

Confirm all four are healthy:
```bash
for p in 8080 8888 8092 8093; do curl -s -w "$p %{http_code}\n" -o /dev/null http://localhost:$p/actuator/health; done
```

The chef-service migration log should show:
```
Migrating schema "chefs" to version "23 - create services leg schema"
Successfully applied 1 migration
```

## Step 2 — Get a vendor JWT

```bash
# Request OTP (dev mode hardcodes 123456)
curl -X POST http://localhost:8080/api/v1/auth/request-otp \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919999900001","purpose":"LOGIN"}'

# Verify and get tokens
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919999900001","otp":"123456"}'
# Response: { "accessToken": "...", "refreshToken": "...", "userId": "..." }

export VENDOR_TOKEN=<accessToken>
export VENDOR_ID=<userId>
```

## Step 3 — Create DRAFT cake listing

```bash
curl -X POST http://localhost:8080/api/v1/services/listings \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceType": "CAKE_DESIGNER",
    "businessName": "Sweet Symphony Bakery",
    "vendorSlug": "sweet-symphony-bakery",
    "tagline": "Bespoke wedding cakes since 2018",
    "homeCity": "Hyderabad",
    "homePincode": "500032",
    "deliveryRadiusKm": 25,
    "outstationCapable": false,
    "pricingPattern": "PER_UNIT_TIERED",
    "calendarMode": "DAY_GRAIN",
    "defaultLeadTimeHours": 48,
    "typeAttributes": {
      "bakeryType": "HOME_BAKER",
      "ovenCapacityKgPerDay": 15,
      "maxTierCount": 5,
      "egglessCapable": true,
      "veganCapable": false,
      "flavoursOffered": ["CHOCOLATE","VANILLA","RED_VELVET","FRESH_FRUIT"],
      "designStyles": ["FONDANT","BUTTERCREAM"],
      "deliveryMode": "SELF"
    }
  }'
# Response: { "id": "...", "status": "DRAFT", ... }

export LISTING_ID=<id>
```

## Step 4 — Try to submit before KYC docs (should fail)

```bash
curl -X POST http://localhost:8080/api/v1/services/listings/$LISTING_ID/submit \
  -H "Authorization: Bearer $VENDOR_TOKEN"
# Expected: 400 — "Missing required KYC documents for CAKE_DESIGNER: [AADHAAR, PAN, FSSAI]"
```

This proves the **KYC gate is enforced**.

## Step 5 — Upload required KYC docs

```bash
for type in AADHAAR PAN FSSAI; do
  curl -X POST http://localhost:8080/api/v1/services/listings/$LISTING_ID/kyc-documents \
    -H "Authorization: Bearer $VENDOR_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"documentType\":\"$type\",\"documentUrl\":\"https://example.com/doc-$type.pdf\",\"documentNumber\":\"TEST-$type-001\"}"
  echo
done
```

## Step 6 — Submit (should succeed now)

```bash
curl -X POST http://localhost:8080/api/v1/services/listings/$LISTING_ID/submit \
  -H "Authorization: Bearer $VENDOR_TOKEN"
# Expected: 200, status: PENDING_REVIEW
```

## Step 7 — Get an admin JWT and approve

The admin is a user with `ROLE_ADMIN`. Use the existing admin login flow (see `admin/` README) to get an `admin_token`, then:

```bash
export ADMIN_TOKEN=<admin token>

# Verify the listing is in the admin queue
curl http://localhost:8080/api/v1/services/admin/listings?status=PENDING_REVIEW \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Approve
curl -X POST http://localhost:8080/api/v1/services/admin/listings/$LISTING_ID/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# Expected: 200, status: VERIFIED
```

## Step 8 — Confirm listing is publicly visible

```bash
# Public browse (no auth)
curl http://localhost:8080/api/v1/services/listings?serviceType=CAKE_DESIGNER&city=Hyderabad

# Public storefront by slug
curl http://localhost:8080/api/v1/services/listings/by-slug/sweet-symphony-bakery
```

## Frontend smoke

- Vendor wizard: `http://localhost:3000/vendor/onboard/cake` (after `npm run dev` in safar-web)
- Vendor dashboard: `http://localhost:3000/vendor/dashboard`
- Admin queue: `http://localhost:3001/service-listings` (after `npm run dev` in admin)

## Troubleshooting

- **401 on submit:** auth-service or api-gateway not running, or JWT expired. Re-run Step 2.
- **"Unknown serviceType":** Check spelling — must be `CAKE_DESIGNER`, `SINGER`, `PANDIT`, `DECORATOR`, `STAFF_HIRE`.
- **Hibernate validate failure on startup:** V23 didn't apply or column types diverged. Check Flyway log and `services.service_listings` schema.
- **vendor_slug uniqueness violation:** Slug already taken; use a different one.
- **Admin queue empty:** confirm listing's `status = 'PENDING_REVIEW'` (look in DB or in the vendor's `/me` response).
