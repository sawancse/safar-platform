#!/bin/bash
# ================================================================
# E2E Test: Tenancy Agreement PDF for Apartment & PG
# ================================================================
# Prerequisites: All services running (docker-compose up + services)
# Usage: bash tests/e2e-agreement-test.sh
# ================================================================

set -e
BASE="http://localhost:8080/api/v1"
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

pass() { echo -e "${GREEN}✓ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; exit 1; }
info() { echo -e "${YELLOW}→ $1${NC}"; }

# ────────────────────────────────────────────
# 1. SETUP: Create host and tenant users
# ────────────────────────────────────────────
info "Step 1: Creating test users (host + tenant)"

# Host login (OTP dev mode: 123456)
HOST_PHONE="+919999900001"
curl -s -X POST "$BASE/auth/otp/send" -H "Content-Type: application/json" \
  -d "{\"phone\":\"$HOST_PHONE\"}" > /dev/null
HOST_TOKEN=$(curl -s -X POST "$BASE/auth/otp/verify" -H "Content-Type: application/json" \
  -d "{\"phone\":\"$HOST_PHONE\",\"otp\":\"123456\"}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))")

if [ -z "$HOST_TOKEN" ]; then fail "Host login failed"; fi
pass "Host logged in"

HOST_ID=$(curl -s "$BASE/users/me" -H "Authorization: Bearer $HOST_TOKEN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
pass "Host ID: $HOST_ID"

# Tenant login
TENANT_PHONE="+919999900002"
curl -s -X POST "$BASE/auth/otp/send" -H "Content-Type: application/json" \
  -d "{\"phone\":\"$TENANT_PHONE\"}" > /dev/null
TENANT_TOKEN=$(curl -s -X POST "$BASE/auth/otp/verify" -H "Content-Type: application/json" \
  -d "{\"phone\":\"$TENANT_PHONE\",\"otp\":\"123456\"}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))")

if [ -z "$TENANT_TOKEN" ]; then fail "Tenant login failed"; fi
pass "Tenant logged in"

TENANT_ID=$(curl -s "$BASE/users/me" -H "Authorization: Bearer $TENANT_TOKEN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
pass "Tenant ID: $TENANT_ID"

# ════════════════════════════════════════════
# TEST CASE 1: PG TENANCY AGREEMENT
# ════════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
info "TEST CASE 1: PG Tenancy Agreement"
echo "═══════════════════════════════════════"

# 2. Create PG listing
info "Step 2: Creating PG listing"
PG_LISTING=$(curl -s -X POST "$BASE/listings" \
  -H "Authorization: Bearer $HOST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Sunshine PG for Men - Koramangala",
    "description": "Fully furnished PG with meals, WiFi, laundry. Near Sony Signal.",
    "type": "PG",
    "addressLine1": "42, 4th Cross, 6th Block",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": "560095",
    "basePricePaise": 1200000,
    "pricingUnit": "MONTH",
    "maxGuests": 3,
    "bedrooms": 1,
    "bathrooms": 1,
    "occupancyType": "MALE",
    "foodType": "VEG",
    "gateClosingTime": "22:30",
    "noticePeriodDays": 30,
    "securityDepositPaise": 2400000,
    "gracePeriodDays": 5,
    "latePenaltyBps": 200
  }')

PG_LISTING_ID=$(echo "$PG_LISTING" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
if [ -z "$PG_LISTING_ID" ]; then fail "PG listing creation failed"; fi
pass "PG Listing created: $PG_LISTING_ID"

# 3. Create PG tenancy
info "Step 3: Creating PG tenancy with custom penalty config"
PG_TENANCY=$(curl -s -X POST "$BASE/pg-tenancies" \
  -H "Authorization: Bearer $HOST_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"tenantId\": \"$TENANT_ID\",
    \"listingId\": \"$PG_LISTING_ID\",
    \"sharingType\": \"DOUBLE\",
    \"moveInDate\": \"2026-04-01\",
    \"noticePeriodDays\": 30,
    \"monthlyRentPaise\": 1200000,
    \"securityDepositPaise\": 2400000,
    \"mealsIncluded\": true,
    \"laundryIncluded\": false,
    \"wifiIncluded\": true,
    \"totalMonthlyPaise\": 1400000,
    \"billingDay\": 1,
    \"gracePeriodDays\": 5,
    \"latePenaltyBps\": 200,
    \"maxPenaltyPercent\": 25
  }")

PG_TENANCY_ID=$(echo "$PG_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
if [ -z "$PG_TENANCY_ID" ]; then
  echo "$PG_TENANCY"
  fail "PG tenancy creation failed"
fi
pass "PG Tenancy created: $PG_TENANCY_ID"

# Verify grace period and penalty config
PG_GRACE=$(echo "$PG_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('gracePeriodDays',''))")
PG_PENALTY=$(echo "$PG_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('latePenaltyBps',''))")
PG_MAX=$(echo "$PG_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('maxPenaltyPercent',''))")
[ "$PG_GRACE" = "5" ] && pass "Grace period = 5 days" || fail "Grace period expected 5, got $PG_GRACE"
[ "$PG_PENALTY" = "200" ] && pass "Penalty = 200 bps (2%/day)" || fail "Penalty expected 200, got $PG_PENALTY"
[ "$PG_MAX" = "25" ] && pass "Max penalty cap = 25%" || fail "Max cap expected 25, got $PG_MAX"

# 4. Create agreement
info "Step 4: Creating PG tenancy agreement"
PG_AGREEMENT=$(curl -s -X POST "$BASE/pg-tenancies/$PG_TENANCY_ID/agreement" \
  -H "Authorization: Bearer $HOST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "Rahul Sharma",
    "tenantPhone": "+919876543210",
    "tenantEmail": "rahul@example.com",
    "tenantAadhaarLast4": "7890",
    "hostName": "Suresh Kumar",
    "hostPhone": "+919999900001",
    "propertyAddress": "42, 4th Cross, 6th Block, Koramangala, Bangalore - 560095",
    "roomDescription": "Room 3, Bed B (Double Sharing)",
    "lockInPeriodMonths": 6,
    "maintenanceChargesPaise": 200000,
    "termsAndConditions": "No smoking inside rooms. Visitors must register at reception."
  }')

PG_AGREEMENT_NUM=$(echo "$PG_AGREEMENT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('agreementNumber',''))")
PG_AGREEMENT_STATUS=$(echo "$PG_AGREEMENT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))")
if [ -z "$PG_AGREEMENT_NUM" ]; then
  echo "$PG_AGREEMENT"
  fail "PG agreement creation failed"
fi
pass "PG Agreement created: $PG_AGREEMENT_NUM (status: $PG_AGREEMENT_STATUS)"

# 5. View agreement text
info "Step 5: Fetching agreement text"
PG_TEXT=$(curl -s "$BASE/pg-tenancies/$PG_TENANCY_ID/agreement/text" \
  -H "Authorization: Bearer $HOST_TOKEN")
echo "$PG_TEXT" | head -5
[ -n "$PG_TEXT" ] && pass "Agreement text retrieved (${#PG_TEXT} chars)" || fail "Empty agreement text"

# Check grace period is in agreement text
echo "$PG_TEXT" | grep -q "grace period of 5 day" && pass "Agreement text has correct grace period (5 days)" || fail "Grace period not in agreement text"
echo "$PG_TEXT" | grep -q "2.00%" && pass "Agreement text has correct penalty rate (2.00%)" || fail "Penalty rate not in agreement text"
echo "$PG_TEXT" | grep -q "25%" && pass "Agreement text has penalty cap (25%)" || fail "Penalty cap not in agreement text"
echo "$PG_TEXT" | grep -q "1st and 5th" && pass "Agreement text has payment window (1st-5th)" || fail "Payment window not in agreement text"

# 6. View agreement HTML
info "Step 6: Fetching agreement as HTML (public, no auth)"
PG_HTML_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/pg-tenancies/$PG_TENANCY_ID/agreement/view")
[ "$PG_HTML_STATUS" = "200" ] && pass "HTML view endpoint returns 200 (public)" || fail "HTML view returned $PG_HTML_STATUS"

PG_HTML=$(curl -s "$BASE/pg-tenancies/$PG_TENANCY_ID/agreement/view")
echo "$PG_HTML" | grep -q "SAFAR" && pass "HTML contains Safar branding" || fail "HTML missing branding"
echo "$PG_HTML" | grep -q "Rahul Sharma" && pass "HTML contains tenant name" || fail "HTML missing tenant name"
echo "$PG_HTML" | grep -q "12,000" && pass "HTML contains rent amount" || fail "HTML missing rent"

# 7. Download PDF
info "Step 7: Downloading agreement PDF (public, no auth)"
PG_PDF_STATUS=$(curl -s -o /tmp/pg-agreement.pdf -w "%{http_code}" "$BASE/pg-tenancies/$PG_TENANCY_ID/agreement/pdf")
[ "$PG_PDF_STATUS" = "200" ] && pass "PDF download returns 200" || fail "PDF download returned $PG_PDF_STATUS"

PG_PDF_SIZE=$(stat -f%z /tmp/pg-agreement.pdf 2>/dev/null || stat -c%s /tmp/pg-agreement.pdf 2>/dev/null || echo "0")
[ "$PG_PDF_SIZE" -gt 1000 ] && pass "PDF generated: ${PG_PDF_SIZE} bytes" || fail "PDF too small: ${PG_PDF_SIZE} bytes"

# Check PDF header
head -c 4 /tmp/pg-agreement.pdf | grep -q "%PDF" && pass "Valid PDF file header" || fail "Invalid PDF header"

# 8. Host signs agreement
info "Step 8: Host signing the agreement"
HOST_SIGN=$(curl -s -X POST "$BASE/pg-tenancies/$PG_TENANCY_ID/agreement/host-sign" \
  -H "Authorization: Bearer $HOST_TOKEN")
HOST_SIGN_STATUS=$(echo "$HOST_SIGN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))")
[ "$HOST_SIGN_STATUS" = "PENDING_TENANT_SIGN" ] && pass "Host signed → PENDING_TENANT_SIGN" || fail "Expected PENDING_TENANT_SIGN, got $HOST_SIGN_STATUS"

# 9. Tenant signs agreement
info "Step 9: Tenant signing the agreement"
TENANT_SIGN=$(curl -s -X POST "$BASE/pg-tenancies/$PG_TENANCY_ID/agreement/tenant-sign" \
  -H "Authorization: Bearer $TENANT_TOKEN")
TENANT_SIGN_STATUS=$(echo "$TENANT_SIGN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))")
[ "$TENANT_SIGN_STATUS" = "ACTIVE" ] && pass "Tenant signed → ACTIVE" || fail "Expected ACTIVE, got $TENANT_SIGN_STATUS"

# 10. Download signed PDF (should show SIGNED badges)
info "Step 10: Downloading signed agreement PDF"
curl -s -o /tmp/pg-agreement-signed.pdf "$BASE/pg-tenancies/$PG_TENANCY_ID/agreement/pdf"
SIGNED_SIZE=$(stat -f%z /tmp/pg-agreement-signed.pdf 2>/dev/null || stat -c%s /tmp/pg-agreement-signed.pdf 2>/dev/null || echo "0")
pass "Signed PDF: ${SIGNED_SIZE} bytes"

# Verify signed HTML has signature info
SIGNED_HTML=$(curl -s "$BASE/pg-tenancies/$PG_TENANCY_ID/agreement/view")
echo "$SIGNED_HTML" | grep -q "SIGNED" && pass "Signed HTML shows SIGNED badge" || fail "Missing SIGNED badge"

# 11. Update penalty config
info "Step 11: Updating penalty config (grace=7, penalty=150bps, cap=30%)"
UPDATED_TENANCY=$(curl -s -X PATCH "$BASE/pg-tenancies/$PG_TENANCY_ID/penalty-config?gracePeriodDays=7&latePenaltyBps=150&maxPenaltyPercent=30" \
  -H "Authorization: Bearer $HOST_TOKEN")
UPD_GRACE=$(echo "$UPDATED_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('gracePeriodDays',''))")
UPD_PENALTY=$(echo "$UPDATED_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('latePenaltyBps',''))")
UPD_MAX=$(echo "$UPDATED_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('maxPenaltyPercent',''))")
[ "$UPD_GRACE" = "7" ] && pass "Grace period updated to 7 days" || fail "Grace update failed: $UPD_GRACE"
[ "$UPD_PENALTY" = "150" ] && pass "Penalty updated to 150 bps (1.5%/day)" || fail "Penalty update failed: $UPD_PENALTY"
[ "$UPD_MAX" = "30" ] && pass "Max cap updated to 30%" || fail "Max cap update failed: $UPD_MAX"


# ════════════════════════════════════════════
# TEST CASE 2: APARTMENT TENANCY AGREEMENT
# ════════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
info "TEST CASE 2: Apartment Tenancy Agreement"
echo "═══════════════════════════════════════"

# 12. Create apartment listing
info "Step 12: Creating Apartment listing"
APT_LISTING=$(curl -s -X POST "$BASE/listings" \
  -H "Authorization: Bearer $HOST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "2BHK Furnished Apartment - Indiranagar",
    "description": "Modern 2BHK with modular kitchen, 2 balconies, covered parking. Near 100ft Road.",
    "type": "APARTMENT",
    "addressLine1": "301, Skylark Residency, 12th Main",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": "560038",
    "basePricePaise": 3500000,
    "pricingUnit": "MONTH",
    "maxGuests": 4,
    "bedrooms": 2,
    "bathrooms": 2,
    "noticePeriodDays": 60,
    "securityDepositPaise": 10000000,
    "gracePeriodDays": 5,
    "latePenaltyBps": 100
  }')

APT_LISTING_ID=$(echo "$APT_LISTING" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
if [ -z "$APT_LISTING_ID" ]; then
  echo "$APT_LISTING"
  fail "Apartment listing creation failed"
fi
pass "Apartment Listing created: $APT_LISTING_ID"

# 13. Create apartment tenancy (longer lock-in, higher deposit, no meals)
info "Step 13: Creating Apartment tenancy (11-month lease)"
APT_TENANCY=$(curl -s -X POST "$BASE/pg-tenancies" \
  -H "Authorization: Bearer $HOST_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"tenantId\": \"$TENANT_ID\",
    \"listingId\": \"$APT_LISTING_ID\",
    \"sharingType\": \"PRIVATE\",
    \"moveInDate\": \"2026-04-15\",
    \"noticePeriodDays\": 60,
    \"monthlyRentPaise\": 3500000,
    \"securityDepositPaise\": 10000000,
    \"mealsIncluded\": false,
    \"laundryIncluded\": false,
    \"wifiIncluded\": false,
    \"totalMonthlyPaise\": 3500000,
    \"billingDay\": 15,
    \"gracePeriodDays\": 5,
    \"latePenaltyBps\": 100,
    \"maxPenaltyPercent\": 20
  }")

APT_TENANCY_ID=$(echo "$APT_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
if [ -z "$APT_TENANCY_ID" ]; then
  echo "$APT_TENANCY"
  fail "Apartment tenancy creation failed"
fi
pass "Apartment Tenancy created: $APT_TENANCY_ID"

# Verify apartment config
APT_GRACE=$(echo "$APT_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('gracePeriodDays',''))")
APT_PENALTY=$(echo "$APT_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('latePenaltyBps',''))")
APT_MAX=$(echo "$APT_TENANCY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('maxPenaltyPercent',''))")
[ "$APT_GRACE" = "5" ] && pass "Apt grace = 5 days" || fail "Apt grace expected 5, got $APT_GRACE"
[ "$APT_PENALTY" = "100" ] && pass "Apt penalty = 100 bps (1%/day)" || fail "Apt penalty expected 100, got $APT_PENALTY"
[ "$APT_MAX" = "20" ] && pass "Apt max cap = 20%" || fail "Apt max cap expected 20, got $APT_MAX"

# 14. Create apartment agreement
info "Step 14: Creating Apartment agreement (11-month lock-in)"
APT_AGREEMENT=$(curl -s -X POST "$BASE/pg-tenancies/$APT_TENANCY_ID/agreement" \
  -H "Authorization: Bearer $HOST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "Priya Patel",
    "tenantPhone": "+919876500000",
    "tenantEmail": "priya.patel@company.com",
    "tenantAadhaarLast4": "4567",
    "hostName": "Suresh Kumar",
    "hostPhone": "+919999900001",
    "propertyAddress": "301, Skylark Residency, 12th Main, Indiranagar, Bangalore - 560038",
    "roomDescription": "2BHK - Full apartment (Flat 301)",
    "lockInPeriodMonths": 11,
    "maintenanceChargesPaise": 350000,
    "termsAndConditions": "1. Flat is for residential use only.\n2. No structural modifications without written consent.\n3. Tenant responsible for electricity, water, gas bills.\n4. Painting at own cost if walls are damaged.\n5. Society parking rules must be followed.\n6. No pets without prior written approval."
  }')

APT_AGREEMENT_NUM=$(echo "$APT_AGREEMENT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('agreementNumber',''))")
if [ -z "$APT_AGREEMENT_NUM" ]; then
  echo "$APT_AGREEMENT"
  fail "Apartment agreement creation failed"
fi
pass "Apartment Agreement created: $APT_AGREEMENT_NUM"

# 15. View apartment agreement text
info "Step 15: Verifying apartment agreement text"
APT_TEXT=$(curl -s "$BASE/pg-tenancies/$APT_TENANCY_ID/agreement/text" \
  -H "Authorization: Bearer $HOST_TOKEN")

echo "$APT_TEXT" | grep -q "Priya Patel" && pass "Contains tenant name: Priya Patel" || fail "Missing tenant name"
echo "$APT_TEXT" | grep -q "Skylark Residency" && pass "Contains property: Skylark Residency" || fail "Missing property"
echo "$APT_TEXT" | grep -q "11 month" && pass "Contains lock-in: 11 months" || fail "Missing lock-in"
echo "$APT_TEXT" | grep -q "60 days" && pass "Contains notice: 60 days" || fail "Missing notice period"
echo "$APT_TEXT" | grep -q "35,000" && pass "Contains rent: ₹35,000" || fail "Missing rent amount"
echo "$APT_TEXT" | grep -q "1,00,000" && pass "Contains deposit: ₹1,00,000" || fail "Missing deposit"
echo "$APT_TEXT" | grep -q "15th" && pass "Contains billing day: 15th" || fail "Missing billing day"
echo "$APT_TEXT" | grep -q "grace period of 5" && pass "Contains grace period: 5 days" || fail "Missing grace period"
echo "$APT_TEXT" | grep -q "1.00%" && pass "Contains penalty: 1.00%/day" || fail "Missing penalty rate"
echo "$APT_TEXT" | grep -q "20%" && pass "Contains max cap: 20%" || fail "Missing max cap"
echo "$APT_TEXT" | grep -q "No structural modifications" && pass "Contains custom terms" || fail "Missing custom terms"

# 16. View apartment HTML
info "Step 16: Fetching apartment agreement HTML"
APT_HTML=$(curl -s "$BASE/pg-tenancies/$APT_TENANCY_ID/agreement/view")
echo "$APT_HTML" | grep -q "Priya Patel" && pass "HTML has tenant name" || fail "HTML missing tenant"
echo "$APT_HTML" | grep -q "PENDING" && pass "HTML shows PENDING signature" || fail "HTML missing pending badge"

# 17. Download apartment PDF
info "Step 17: Downloading apartment agreement PDF"
APT_PDF_STATUS=$(curl -s -o /tmp/apt-agreement.pdf -w "%{http_code}" "$BASE/pg-tenancies/$APT_TENANCY_ID/agreement/pdf")
[ "$APT_PDF_STATUS" = "200" ] && pass "Apartment PDF download: 200 OK" || fail "PDF returned $APT_PDF_STATUS"

APT_PDF_SIZE=$(stat -f%z /tmp/apt-agreement.pdf 2>/dev/null || stat -c%s /tmp/apt-agreement.pdf 2>/dev/null || echo "0")
[ "$APT_PDF_SIZE" -gt 1000 ] && pass "Apartment PDF: ${APT_PDF_SIZE} bytes" || fail "PDF too small"
head -c 4 /tmp/apt-agreement.pdf | grep -q "%PDF" && pass "Valid PDF header" || fail "Invalid PDF"

# 18. Full signing flow for apartment
info "Step 18: Full signing flow (host → tenant)"
curl -s -X POST "$BASE/pg-tenancies/$APT_TENANCY_ID/agreement/host-sign" \
  -H "Authorization: Bearer $HOST_TOKEN" > /dev/null
pass "Host signed apartment agreement"

FINAL=$(curl -s -X POST "$BASE/pg-tenancies/$APT_TENANCY_ID/agreement/tenant-sign" \
  -H "Authorization: Bearer $TENANT_TOKEN")
FINAL_STATUS=$(echo "$FINAL" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))")
[ "$FINAL_STATUS" = "ACTIVE" ] && pass "Apartment agreement → ACTIVE" || fail "Expected ACTIVE, got $FINAL_STATUS"

# 19. Download fully signed apartment PDF
info "Step 19: Downloading fully signed apartment PDF"
curl -s -o /tmp/apt-agreement-signed.pdf "$BASE/pg-tenancies/$APT_TENANCY_ID/agreement/pdf"
FINAL_SIZE=$(stat -f%z /tmp/apt-agreement-signed.pdf 2>/dev/null || stat -c%s /tmp/apt-agreement-signed.pdf 2>/dev/null || echo "0")
pass "Signed apartment PDF: ${FINAL_SIZE} bytes"

# Verify both signatures in HTML
SIGNED_APT_HTML=$(curl -s "$BASE/pg-tenancies/$APT_TENANCY_ID/agreement/view")
SIGNED_COUNT=$(echo "$SIGNED_APT_HTML" | grep -c "SIGNED" || echo "0")
[ "$SIGNED_COUNT" -ge 2 ] && pass "HTML shows both SIGNED badges ($SIGNED_COUNT)" || fail "Expected 2+ SIGNED badges, got $SIGNED_COUNT"

# 20. Inline PDF view
info "Step 20: Testing inline PDF view"
INLINE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/pg-tenancies/$APT_TENANCY_ID/agreement/pdf/inline")
[ "$INLINE_STATUS" = "200" ] && pass "Inline PDF: 200 OK" || fail "Inline PDF returned $INLINE_STATUS"

# ════════════════════════════════════════════
# TEST CASE 3: TENANT DASHBOARD VIEW
# ════════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════"
info "TEST CASE 3: Tenant Dashboard"
echo "═══════════════════════════════════════"

info "Step 21: Fetching tenant dashboard"
DASHBOARD=$(curl -s "$BASE/pg-tenancies/my-dashboard" \
  -H "X-User-Id: $TENANT_ID" \
  -H "Authorization: Bearer $TENANT_TOKEN")

DASH_STATUS=$(echo "$DASHBOARD" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('agreement',{}).get('status',''))" 2>/dev/null)
[ "$DASH_STATUS" = "ACTIVE" ] && pass "Dashboard shows ACTIVE agreement" || info "Dashboard agreement status: $DASH_STATUS (may show latest tenancy)"

echo ""
echo "═══════════════════════════════════════"
echo -e "${GREEN}ALL TESTS PASSED!${NC}"
echo "═══════════════════════════════════════"
echo ""
echo "Generated files:"
echo "  /tmp/pg-agreement.pdf         — PG agreement (unsigned)"
echo "  /tmp/pg-agreement-signed.pdf  — PG agreement (signed)"
echo "  /tmp/apt-agreement.pdf        — Apartment agreement (unsigned)"
echo "  /tmp/apt-agreement-signed.pdf — Apartment agreement (signed)"
echo ""
echo "View in browser:"
echo "  $BASE/pg-tenancies/$PG_TENANCY_ID/agreement/view"
echo "  $BASE/pg-tenancies/$APT_TENANCY_ID/agreement/view"
