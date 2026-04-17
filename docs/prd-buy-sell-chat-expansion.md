# PRD: Buy/Sell Property Expansion, Enhanced Chat & Builder Booking

**Version:** 1.0
**Date:** 2026-04-12
**Status:** Draft

---

## 1. Executive Summary

This PRD covers four interconnected features that transform Safar from a rental-only platform to a full-spectrum real estate marketplace:

1. **Enhanced Chat** — File sharing (PDF, docs, images) in all conversations + dedicated chat for agreements/plans
2. **Chef-Cook Chat** — Messaging with live location sharing during bookings
3. **Builder Booking** — Express interest with online token/booking amount via Razorpay
4. **Buy/Sell Expansion** — Land, villa, farmhouse, farming land, individual house + agent chat

---

## 2. Market Research Summary

### Competitive Landscape

| Feature | NoBroker | 99acres | Zillow | PropertyGuru | **Safar (New)** |
|---------|----------|---------|--------|-------------|-----------------|
| Property types for sale | 8 | 12+ | 8 | 7 | **14** |
| Agricultural/farm land | No | Yes | Ranch only | No | **Yes** |
| Online token payment | No | No | No | No | **Yes (1-2% via Razorpay)** |
| Chat with file sharing | No | No | No | No | **Yes** |
| Live location in chat | No | No | No | No | **Yes (chef)** |
| In-app offer/negotiate | No | No | Yes | No | **Yes** |
| RERA verification | Yes | Yes | N/A | N/A | **Yes** |
| Locality price trends | No | Yes | Zestimate | Yes | **Phase 2** |

### Key Differentiators

1. **Online token money** — No Indian platform supports this. 1-2% refundable booking via Razorpay held in escrow.
2. **Chat with document sharing** — Floor plans, brochures, agreements shared natively in chat (not email/WhatsApp).
3. **Broadest Indian coverage** — 14 property types including agricultural land, farmhouse, farming land.

---

## 3. Feature 1: Enhanced Chat with File Sharing

### Problem
Current messaging-service supports text only. Hosts/tenants need to share agreements, floor plans, brochures, and property documents. Currently done via WhatsApp/email outside the platform.

### Requirements

#### Message Attachments
- **Supported types:** PDF, DOC/DOCX, JPG, PNG, WebP (max 10MB per file)
- **Upload flow:** User selects file → upload to media-service (S3) → send message with attachment URL
- **Message types expanded:** TEXT, SYSTEM, BOOKING_UPDATE, **FILE, IMAGE, LOCATION**
- **File metadata:** `fileName`, `fileSize`, `mimeType`, `attachmentUrl`

#### Agreement/Plan Review in Chat
- When host sends agreement for review, message type = FILE with context `agreementReview`
- Tenant can accept/reject inline (action buttons in chat bubble)
- Agreement PDF auto-attached when status changes to PENDING_TENANT_SIGN

#### Schema Changes (messaging-service)
```sql
ALTER TABLE messaging.messages ADD COLUMN attachment_url TEXT;
ALTER TABLE messaging.messages ADD COLUMN attachment_name VARCHAR(255);
ALTER TABLE messaging.messages ADD COLUMN attachment_size BIGINT;
ALTER TABLE messaging.messages ADD COLUMN attachment_type VARCHAR(50);  -- mime type
ALTER TABLE messaging.messages ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE messaging.messages ADD COLUMN longitude DOUBLE PRECISION;
-- Expand message_type enum: TEXT, SYSTEM, BOOKING_UPDATE, FILE, IMAGE, LOCATION
ALTER TABLE messaging.messages ALTER COLUMN message_type TYPE VARCHAR(30);
```

#### API Changes
- `POST /api/v1/messages` — Accept multipart/form-data OR JSON with `attachmentUrl`
- `POST /api/v1/messages/upload` — Upload file to S3 via media-service, return URL
- Response includes attachment metadata

---

## 4. Feature 2: Chef-Cook Chat with Live Location

### Problem
Chef tracking exists (lat/lng on ChefBooking) but there's no in-app chat between customer and chef. Location sharing is API-only with no real-time feed.

### Requirements

#### Chef-Customer Chat
- Auto-create conversation when chef booking is CONFIRMED
- `conversationId` linked via `bookingId` in messaging-service
- Chef can send text + location messages
- Customer sees location on embedded map in chat

#### Live Location Sharing
- New message type: `LOCATION` with `latitude`, `longitude` fields
- Chef shares live location → creates LOCATION message every 30 seconds while active
- Frontend renders as embedded Google Maps snippet in chat bubble
- ETA shown in location bubble ("Chef is 15 min away")

#### Flow
1. Chef booking confirmed → auto-create conversation (Kafka: `chef.booking.confirmed` → messaging-service)
2. Chef opens chat → can send text + share live location
3. Customer sees chat with location pins + ETA
4. Location sharing auto-stops when booking status = COMPLETED

---

## 5. Feature 3: Builder Project — Express Interest with Booking Amount

### Problem
Current flow: buyer creates PropertyInquiry (free) → seller contacts offline. No payment commitment. Builders want serious buyers with skin in the game.

### Requirements

#### Online Token/Booking Amount
- Builder sets `bookingAmountPaise` on project (default: 1% of min price, range: ₹10,000–₹5,00,000)
- Buyer flow:
  1. Browse project → click "Express Interest"
  2. Fill inquiry form (name, phone, budget, preferred unit type, visit date)
  3. Option: "Book Now" → pay token amount via Razorpay
  4. Token held in Safar escrow (payment-service)
  5. Refundable within 30 days if buyer cancels

#### PropertyInquiry Enhancement
```sql
ALTER TABLE listings.property_inquiries ADD COLUMN token_amount_paise BIGINT DEFAULT 0;
ALTER TABLE listings.property_inquiries ADD COLUMN payment_status VARCHAR(20); -- NONE, PAID, REFUNDED
ALTER TABLE listings.property_inquiries ADD COLUMN razorpay_payment_id VARCHAR(100);
ALTER TABLE listings.property_inquiries ADD COLUMN razorpay_refund_id VARCHAR(100);
ALTER TABLE listings.property_inquiries ADD COLUMN paid_at TIMESTAMPTZ;
ALTER TABLE listings.property_inquiries ADD COLUMN refunded_at TIMESTAMPTZ;
ALTER TABLE listings.property_inquiries ADD COLUMN unit_type_id UUID;
ALTER TABLE listings.property_inquiries ADD COLUMN preferred_floor VARCHAR(20);
```

#### API Changes
- `POST /api/v1/inquiries` — Accept optional `tokenAmountPaise` and `paymentMode`
- `POST /api/v1/inquiries/{id}/pay` — Initiate Razorpay payment for token
- `POST /api/v1/inquiries/{id}/refund` — Refund token (buyer request within 30 days)
- Webhook: `inquiry.token.paid`, `inquiry.token.refunded`

#### Auto-Chat on Interest
- When inquiry created → auto-create messaging conversation between buyer and builder/seller
- Buyer's inquiry message auto-sent as first chat message
- Builder can respond in chat, share brochure (PDF), floor plans

---

## 6. Feature 4: Buy/Sell Property Expansion

### Problem
Current `SalePropertyType` enum covers apartments, villas, plots, and commercial. Missing: agricultural land, farming land, farmhouse, individual house (non-villa), and several categories that 99acres/MagicBricks support.

### New Property Types

| Type | Enum Value | Category | Specific Fields |
|------|-----------|----------|-----------------|
| Residential Plot | `RESIDENTIAL_PLOT` | Land | plotAreaSqft, plotLength, plotBreadth, boundaryWall, cornerPlot |
| Agricultural Land | `AGRICULTURAL_LAND` | Land | totalAcres, irrigationType, soilType, roadAccess, waterSource, cropSuitable |
| Farming Land | `FARMING_LAND` | Land | totalAcres, currentCrop, organicCertified, borewellCount, fencingType |
| Farmhouse | `FARM_HOUSE` | (already exists) | totalAcres, gardenArea, swimmingPool, caretakerRoom |
| Individual House | `INDEPENDENT_HOUSE` | (already exists) | floors, roofType, gardenArea |
| Villa | `VILLA` | (already exists) | — |
| Commercial Land | `COMMERCIAL_LAND` | Land | zoneType, FSI, roadWidth, cornerPlot |
| Industrial Land | `INDUSTRIAL_LAND` | Land | zoneType, powerSupply, waterSupply, wasteDisposal |

### SaleProperty Schema Enhancement
```sql
-- Land-specific fields
ALTER TABLE listings.sale_properties ADD COLUMN total_acres DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN plot_length_ft DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN plot_breadth_ft DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN boundary_wall BOOLEAN DEFAULT false;
ALTER TABLE listings.sale_properties ADD COLUMN corner_plot BOOLEAN DEFAULT false;
ALTER TABLE listings.sale_properties ADD COLUMN road_width_ft DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN road_access VARCHAR(30); -- MAIN_ROAD, INTERNAL, NO_ROAD
ALTER TABLE listings.sale_properties ADD COLUMN zone_type VARCHAR(30); -- RESIDENTIAL, COMMERCIAL, INDUSTRIAL, AGRICULTURAL, MIXED
-- Agriculture-specific
ALTER TABLE listings.sale_properties ADD COLUMN irrigation_type VARCHAR(30); -- BOREWELL, CANAL, RIVER, RAIN_FED, DRIP
ALTER TABLE listings.sale_properties ADD COLUMN soil_type VARCHAR(30); -- BLACK, RED, ALLUVIAL, LATERITE
ALTER TABLE listings.sale_properties ADD COLUMN water_source VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN borewell_count INTEGER DEFAULT 0;
ALTER TABLE listings.sale_properties ADD COLUMN fencing_type VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN organic_certified BOOLEAN DEFAULT false;
ALTER TABLE listings.sale_properties ADD COLUMN current_crop VARCHAR(100);
-- Legal
ALTER TABLE listings.sale_properties ADD COLUMN ownership_type VARCHAR(30); -- FREEHOLD, LEASEHOLD, COOPERATIVE, POWER_OF_ATTORNEY
ALTER TABLE listings.sale_properties ADD COLUMN title_clear BOOLEAN;
ALTER TABLE listings.sale_properties ADD COLUMN encumbrance_free BOOLEAN;
ALTER TABLE listings.sale_properties ADD COLUMN rera_number VARCHAR(50);
ALTER TABLE listings.sale_properties ADD COLUMN govt_approved BOOLEAN DEFAULT false;
-- Enhancement
ALTER TABLE listings.sale_properties ADD COLUMN virtual_tour_url TEXT;
ALTER TABLE listings.sale_properties ADD COLUMN brochure_url TEXT;
ALTER TABLE listings.sale_properties ADD COLUMN floor_plan_urls TEXT; -- JSON array
ALTER TABLE listings.sale_properties ADD COLUMN agent_id UUID;
ALTER TABLE listings.sale_properties ADD COLUMN agent_name VARCHAR(100);
ALTER TABLE listings.sale_properties ADD COLUMN agent_phone VARCHAR(20);
```

### NoBroker-Style Listing Format

Following NoBroker's structured approach, each listing requires:

**Mandatory (all types):**
- Property type, transaction type (New/Resale), asking price, location (city/locality/pincode/map pin), photos (min 3), description, ownership type

**Mandatory (built properties):**
- BHK, carpet area, built-up area, floor, total floors, facing, furnishing, age, parking

**Mandatory (land/plot):**
- Plot area (sqft or acres), plot dimensions, boundary wall, corner plot, road access, zone type

**Mandatory (agricultural):**
- Total acres, irrigation, soil type, water source

**Optional (all types):**
- RERA number, virtual tour, brochure, floor plan, amenities, price negotiable

### Chat with Agent
- Every sale property can have an `agentId` (optional)
- Buyer can "Chat with Agent" → creates conversation between buyer and agent
- If no agent, chat goes to seller directly
- Agent can share documents (brochure, legal docs) via file attachment in chat

### Search Integration
- Expand ES `SalePropertyDocument` with new fields
- Add filters: property type (multi-select), price range, area range, land-specific filters
- SmartQueryParser updated to recognize: "farm land", "agricultural", "plot", "farmhouse", "villa", "independent house"

---

## 7. User Flows

### Flow A: Buyer Finds Land, Expresses Interest, Pays Token
```
1. Search "agricultural land near Hyderabad"
2. Browse results with land-specific details (acres, irrigation, soil)
3. Click listing → see full details + photos + map
4. Click "Express Interest" → fill form (budget, timeline, message)
5. Option: "Book with Token (₹25,000)" → Razorpay payment
6. Confirmation → auto-chat created with seller
7. Share documents in chat (title deed, survey map)
8. Schedule visit via chat
```

### Flow B: Chef Shares Location During Cooking Visit
```
1. Chef booking confirmed → chat auto-created
2. Chef clicks "Share Location" in chat → LOCATION message sent
3. Customer sees map pin + "Chef is 15 min away"
4. Chef arrives → stops location sharing
5. Continue chatting for special requests
```

### Flow C: PG Tenant Reviews Agreement in Chat
```
1. Tenant checks in → agreement auto-created (Phase 2 from today)
2. Agreement PDF auto-shared in messaging conversation
3. Tenant reads PDF in chat, asks questions
4. Tenant signs → confirmation message in chat
```

---

## 8. Technical Architecture

### Services Affected
| Service | Changes |
|---------|---------|
| messaging-service | Message entity expansion (attachments, location), upload endpoint |
| media-service | Already handles S3 uploads — reuse for chat attachments |
| listing-service | SaleProperty expansion, PropertyInquiry payment fields, new types |
| payment-service | Inquiry token payment/refund endpoints |
| chef-service | Auto-conversation creation on booking confirm |
| notification-service | Inquiry token payment notifications |
| search-service | ES mapping for expanded sale property fields |
| api-gateway | New routes for chat upload |

### Kafka Events (New)
- `inquiry.token.paid` — token payment captured
- `inquiry.token.refunded` — token refunded
- `inquiry.created` — auto-create chat conversation
- `chef.booking.confirmed` → consumed by messaging-service to auto-create conversation

---

## 9. Success Metrics

| Metric | Target |
|--------|--------|
| Chat file shares per week | 500+ within 3 months |
| Builder token conversions | 5% of inquiries pay token |
| Land/agricultural listings | 200+ within 2 months |
| Chef chat usage rate | 80% of confirmed bookings |

---

## 10. Out of Scope (Phase 2)

- WebSocket real-time messaging (keep polling for now, optimize interval)
- AI property valuation (Zestimate-like)
- Locality price trend charts
- Video call in chat
- Digital offer/counter-offer workflow
