# BMAD Architecture Design: Buy/Sell + Chat + Builder Booking

**Date:** 2026-04-12

---

## 1. System Context

```
[Buyer/Seller/Chef/Tenant]
        |
   [API Gateway :8080]
        |
   ┌────┴────────────────────────────┐
   |    |        |       |     |     |
[listing] [messaging] [payment] [chef] [media]
 :8083     :8091      :8086   :8093  :8088
   |         |          |
 [PostgreSQL]  [S3]   [Razorpay]
   |
 [Elasticsearch]
```

### Data Flow for Key Scenarios

**Chat File Share:**
```
Client → POST /api/v1/messages/upload (multipart)
  → messaging-service → media-service (S3 upload)
  → returns {attachmentUrl, fileName, fileSize, mimeType}
Client → POST /api/v1/messages {content, attachmentUrl, messageType: FILE}
  → messaging-service → save Message → Kafka "message.sent"
```

**Builder Token Payment:**
```
Client → POST /api/v1/inquiries {builderProjectId, tokenAmountPaise}
  → listing-service → save PropertyInquiry (paymentStatus=PENDING)
  → Kafka "inquiry.created" → messaging-service auto-creates conversation
Client → POST /api/v1/inquiries/{id}/pay
  → listing-service → payment-service (Razorpay order create)
  → returns {razorpayOrderId} → client completes payment
Razorpay webhook → payment-service → Kafka "inquiry.token.paid"
  → listing-service updates PropertyInquiry (paymentStatus=PAID)
  → notification-service sends email
```

**Chef Live Location:**
```
Chef → POST /api/v1/chef-bookings/{id}/location {lat, lng, eta}
  → chef-service → update ChefBooking location
  → POST /api/v1/messages {conversationId, messageType: LOCATION, lat, lng}
  → messaging-service → save as LOCATION message
Customer polls chat → sees location message → renders map pin
```

---

## 2. Domain Model Changes

### messaging-service: Message Entity (Expanded)

```java
@Entity
public class Message {
    // Existing
    UUID id;
    UUID conversationId;
    UUID senderId;
    String content;
    String messageType; // TEXT, SYSTEM, BOOKING_UPDATE, FILE, IMAGE, LOCATION
    OffsetDateTime readAt;
    OffsetDateTime createdAt;

    // NEW — Attachment fields
    String attachmentUrl;     // S3 URL
    String attachmentName;    // original filename
    Long attachmentSize;      // bytes
    String attachmentType;    // MIME type (application/pdf, image/jpeg, etc.)

    // NEW — Location fields
    Double latitude;
    Double longitude;
    String locationLabel;     // "Chef is 15 min away"
}
```

### listing-service: SaleProperty Entity (Expanded)

```java
@Entity
public class SaleProperty {
    // Existing fields preserved...

    // NEW — Land/Plot fields
    BigDecimal totalAcres;
    BigDecimal plotLengthFt;
    BigDecimal plotBreadthFt;
    Boolean boundaryWall;
    Boolean cornerPlot;
    BigDecimal roadWidthFt;
    String roadAccess;       // MAIN_ROAD, INTERNAL, NO_ROAD
    String zoneType;         // RESIDENTIAL, COMMERCIAL, INDUSTRIAL, AGRICULTURAL, MIXED

    // NEW — Agriculture fields
    String irrigationType;   // BOREWELL, CANAL, RIVER, RAIN_FED, DRIP
    String soilType;         // BLACK, RED, ALLUVIAL, LATERITE
    String waterSource;
    Integer borewellCount;
    String fencingType;
    Boolean organicCertified;
    String currentCrop;

    // NEW — Legal
    String ownershipType;    // FREEHOLD, LEASEHOLD, COOPERATIVE, POWER_OF_ATTORNEY
    Boolean titleClear;
    Boolean encumbranceFree;
    String reraNumber;
    Boolean govtApproved;

    // NEW — Media
    String virtualTourUrl;
    String brochureUrl;
    String floorPlanUrls;    // JSON array

    // NEW — Agent
    UUID agentId;
    String agentName;
    String agentPhone;
}
```

### SalePropertyType Enum (Expanded)

```java
public enum SalePropertyType {
    // Existing
    APARTMENT, INDEPENDENT_HOUSE, VILLA, PLOT, PENTHOUSE,
    STUDIO, BUILDER_FLOOR, FARM_HOUSE, ROW_HOUSE,
    COMMERCIAL_OFFICE, COMMERCIAL_SHOP, COMMERCIAL_SHOWROOM,
    COMMERCIAL_WAREHOUSE, INDUSTRIAL,
    // NEW
    RESIDENTIAL_PLOT, AGRICULTURAL_LAND, FARMING_LAND,
    COMMERCIAL_LAND, INDUSTRIAL_LAND
}
```

### listing-service: PropertyInquiry Entity (Enhanced)

```java
@Entity
public class PropertyInquiry {
    // Existing fields preserved...

    // NEW — Token payment
    Long tokenAmountPaise;
    String paymentStatus;        // NONE, PENDING, PAID, REFUNDED, EXPIRED
    String razorpayPaymentId;
    String razorpayOrderId;
    String razorpayRefundId;
    OffsetDateTime paidAt;
    OffsetDateTime refundedAt;

    // NEW — Unit preference (builder)
    UUID unitTypeId;
    String preferredFloor;

    // NEW — Chat link
    UUID conversationId;         // auto-created messaging conversation
}
```

---

## 3. API Design

### 3.1 Chat File Upload (messaging-service)

```
POST /api/v1/messages/upload
Content-Type: multipart/form-data
Headers: Authorization: Bearer {token}
Body: file (max 10MB)
Response: {
  attachmentUrl: "https://s3.../chat/uuid/file.pdf",
  fileName: "agreement.pdf",
  fileSize: 245000,
  mimeType: "application/pdf"
}
```

```
POST /api/v1/messages
{
  "listingId": "...",
  "recipientId": "...",
  "content": "Here's the floor plan",
  "messageType": "FILE",            // or IMAGE, LOCATION
  "attachmentUrl": "https://s3...", // for FILE/IMAGE
  "attachmentName": "floorplan.pdf",
  "attachmentSize": 245000,
  "attachmentType": "application/pdf",
  "latitude": 17.385,               // for LOCATION
  "longitude": 78.486
}
```

### 3.2 Builder Token Payment (listing-service + payment-service)

```
POST /api/v1/inquiries
{
  "builderProjectId": "...",
  "message": "Interested in 3BHK, east facing",
  "buyerName": "Rahul",
  "buyerPhone": "9876543210",
  "unitTypeId": "...",
  "preferredFloor": "5-10",
  "tokenAmountPaise": 2500000,   // ₹25,000
  "payWithToken": true
}
Response: { inquiry, razorpayOrderId (if payWithToken) }

POST /api/v1/inquiries/{id}/pay
{ "razorpayPaymentId": "pay_xxx", "razorpaySignature": "..." }
Response: { inquiry with paymentStatus: PAID }

POST /api/v1/inquiries/{id}/refund
Response: { inquiry with paymentStatus: REFUNDED }
```

### 3.3 Chef Location in Chat

```
POST /api/v1/chef-bookings/{id}/share-location
Headers: X-User-Id: {chefUserId}
{
  "lat": 17.385,
  "lng": 78.486,
  "etaMinutes": 15
}
→ Updates ChefBooking location + sends LOCATION message in linked conversation
```

### 3.4 Sale Property (Expanded Filters)

```
GET /api/v1/sale-properties?type=AGRICULTURAL_LAND,FARMING_LAND
  &city=Hyderabad
  &minAcres=2&maxAcres=10
  &irrigationType=BOREWELL
  &minPrice=5000000&maxPrice=20000000
  &ownershipType=FREEHOLD
```

---

## 4. Database Migrations

### messaging-service: V2__chat_attachments.sql
```sql
ALTER TABLE messaging.messages ALTER COLUMN message_type TYPE VARCHAR(30);
ALTER TABLE messaging.messages ADD COLUMN attachment_url TEXT;
ALTER TABLE messaging.messages ADD COLUMN attachment_name VARCHAR(255);
ALTER TABLE messaging.messages ADD COLUMN attachment_size BIGINT;
ALTER TABLE messaging.messages ADD COLUMN attachment_type VARCHAR(50);
ALTER TABLE messaging.messages ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE messaging.messages ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE messaging.messages ADD COLUMN location_label VARCHAR(200);
```

### listing-service: V77__sale_property_expansion.sql
```sql
-- Land fields
ALTER TABLE listings.sale_properties ADD COLUMN total_acres DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN plot_length_ft DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN plot_breadth_ft DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN boundary_wall BOOLEAN DEFAULT false;
ALTER TABLE listings.sale_properties ADD COLUMN corner_plot BOOLEAN DEFAULT false;
ALTER TABLE listings.sale_properties ADD COLUMN road_width_ft DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN road_access VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN zone_type VARCHAR(30);
-- Agriculture
ALTER TABLE listings.sale_properties ADD COLUMN irrigation_type VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN soil_type VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN water_source VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN borewell_count INTEGER DEFAULT 0;
ALTER TABLE listings.sale_properties ADD COLUMN fencing_type VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN organic_certified BOOLEAN DEFAULT false;
ALTER TABLE listings.sale_properties ADD COLUMN current_crop VARCHAR(100);
-- Legal
ALTER TABLE listings.sale_properties ADD COLUMN ownership_type VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN title_clear BOOLEAN;
ALTER TABLE listings.sale_properties ADD COLUMN encumbrance_free BOOLEAN;
ALTER TABLE listings.sale_properties ADD COLUMN rera_number VARCHAR(50);
ALTER TABLE listings.sale_properties ADD COLUMN govt_approved BOOLEAN DEFAULT false;
-- Media
ALTER TABLE listings.sale_properties ADD COLUMN virtual_tour_url TEXT;
ALTER TABLE listings.sale_properties ADD COLUMN brochure_url TEXT;
ALTER TABLE listings.sale_properties ADD COLUMN floor_plan_urls TEXT;
-- Agent
ALTER TABLE listings.sale_properties ADD COLUMN agent_id UUID;
ALTER TABLE listings.sale_properties ADD COLUMN agent_name VARCHAR(100);
ALTER TABLE listings.sale_properties ADD COLUMN agent_phone VARCHAR(20);
```

### listing-service: V78__inquiry_token_payment.sql
```sql
ALTER TABLE listings.property_inquiries ADD COLUMN token_amount_paise BIGINT DEFAULT 0;
ALTER TABLE listings.property_inquiries ADD COLUMN payment_status VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE listings.property_inquiries ADD COLUMN razorpay_payment_id VARCHAR(100);
ALTER TABLE listings.property_inquiries ADD COLUMN razorpay_order_id VARCHAR(100);
ALTER TABLE listings.property_inquiries ADD COLUMN razorpay_refund_id VARCHAR(100);
ALTER TABLE listings.property_inquiries ADD COLUMN paid_at TIMESTAMPTZ;
ALTER TABLE listings.property_inquiries ADD COLUMN refunded_at TIMESTAMPTZ;
ALTER TABLE listings.property_inquiries ADD COLUMN unit_type_id UUID;
ALTER TABLE listings.property_inquiries ADD COLUMN preferred_floor VARCHAR(20);
ALTER TABLE listings.property_inquiries ADD COLUMN conversation_id UUID;
```

---

## 5. Frontend Screens

### Web (safar-web)

| Page | Route | Description |
|------|-------|-------------|
| Chat Inbox | `/messages` | Conversation list with unread badges |
| Chat Thread | `/messages/[conversationId]` | Messages + file upload + location render |
| Sale Property Detail | `/property/[id]` | Enhanced with land fields, agent chat, token booking |
| Sale Listing Form | `/sell/new` | Multi-step wizard, land/agriculture fields appear per type |
| Builder Project | `/builder/[id]` | Express interest + "Book with Token" CTA |
| Chef Tracking | `/chef-booking/[id]` | Chat + embedded map for chef location |

### Mobile (Expo)

| Screen | File | Description |
|--------|------|-------------|
| Chat | `messages.tsx` | Already exists as host-messages, extend for all users |
| File Picker | In chat | DocumentPicker + ImagePicker → upload → send |
| Location Map | In chat | MapView rendering for LOCATION messages |
| Sale Browse | `sale-properties.tsx` | Grid view with type filters |
| Sale Detail | `sale-property-detail.tsx` | Full detail + "Chat with Agent" + "Pay Token" |

---

## 6. Implementation Priority

| Order | Feature | Service | Effort |
|-------|---------|---------|--------|
| 1 | Chat file sharing | messaging-service | Medium |
| 2 | Sale property expansion (entity + migration) | listing-service | Medium |
| 3 | Builder token payment | listing-service + payment | Medium |
| 4 | Chef chat + location | chef-service + messaging | Small |
| 5 | Auto-conversation on inquiry | messaging-service | Small |
| 6 | Frontend: Chat UI (web) | safar-web | Large |
| 7 | Frontend: Sale property pages | safar-web | Large |
| 8 | Frontend: Builder token booking | safar-web | Medium |
| 9 | Search ES expansion for sale | search-service | Medium |

---

## 7. Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Large file uploads | 10MB limit, S3 direct upload with presigned URLs |
| Token refund disputes | 30-day auto-refund window, admin override |
| Chat spam | Rate limit: 30 messages/min per user |
| Agricultural land legal complexity | Disclaimer: "Verify land records independently" |
| Missing WebSocket | Keep 10s polling; upgrade to SSE in Phase 2 |
