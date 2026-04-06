# Chef Service - Mermaid Diagrams

## 1. System Context Diagram

```mermaid
graph TB
    subgraph Clients
        WEB[Web App<br/>Next.js]
        MOB[Mobile App<br/>React Native]
        ADM[Admin Dashboard<br/>React + Vite]
    end

    GW[API Gateway :8080<br/>Spring Cloud Gateway<br/>JWT Auth + Rate Limit]

    subgraph Chef Service [:8093]
        direction TB
        CP[Chef Profiles<br/>12 controllers]
        BK[Bookings<br/>Daily/Monthly]
        EV[Events<br/>Party/Corporate]
        SB[Subscriptions<br/>Monthly Plans]
        PR[Pricing<br/>Event + Cuisine]
        SC[Schedulers<br/>3 cron jobs]
    end

    DB[(PostgreSQL<br/>Schema: chefs<br/>12 tables)]
    KF[Apache Kafka<br/>MSK Serverless<br/>7 topics]
    NS[Notification Service<br/>Email + Push + SMS]
    RZ[Razorpay<br/>Payments]

    WEB --> GW
    MOB --> GW
    ADM --> GW
    GW -->|JWT + X-User-Id| CP
    GW -->|JWT + X-User-Id| BK
    GW -->|JWT + X-User-Id| EV
    GW -->|JWT + X-User-Id| SB
    GW -->|JWT + X-User-Id| PR
    CP --> DB
    BK --> DB
    EV --> DB
    SB --> DB
    PR --> DB
    BK -->|Events| KF
    SC -->|Reminders| KF
    KF --> NS
    BK -.->|Payment Flow| RZ

    style GW fill:#f97316,color:#fff
    style DB fill:#336791,color:#fff
    style KF fill:#231f20,color:#fff
    style NS fill:#10b981,color:#fff
    style RZ fill:#072654,color:#fff
```

## 2. Entity Relationship Diagram

```mermaid
erDiagram
    chef_profiles ||--o{ chef_bookings : "receives"
    chef_profiles ||--o{ event_bookings : "handles"
    chef_profiles ||--o{ chef_subscriptions : "provides"
    chef_profiles ||--o{ chef_menus : "creates"
    chef_profiles ||--o{ chef_availability : "blocks"
    chef_profiles ||--o{ chef_photos : "uploads"
    chef_profiles ||--o{ chef_event_pricing : "customizes"
    chef_profiles ||--o{ cuisine_price_tiers : "sets"
    chef_profiles ||--o{ chef_referrals : "refers"
    chef_menus ||--o{ menu_items : "contains"

    chef_profiles {
        UUID id PK
        UUID userId UK
        string name
        enum chefType "DOMESTIC|PROFESSIONAL|EVENT_SPECIALIST"
        string city
        string cuisines
        long dailyRatePaise
        long monthlyRatePaise
        long eventMinPlatePaise
        double rating
        int reviewCount
        int totalBookings
        enum verificationStatus "PENDING|VERIFIED|REJECTED|SUSPENDED"
        boolean available
        string badge
        string referralCode UK
    }

    chef_bookings {
        UUID id PK
        string bookingRef UK "SC-XXXXXXXX"
        UUID chefId FK
        UUID customerId
        enum serviceType "DAILY|MONTHLY|EVENT"
        enum mealType "BREAKFAST|LUNCH|DINNER|SNACKS|ALL_DAY"
        date serviceDate
        int guestsCount
        long totalAmountPaise
        long advanceAmountPaise "10 pct"
        long platformFeePaise "15 pct"
        string razorpayPaymentId
        enum status "PENDING_PAYMENT|PENDING|CONFIRMED|IN_PROGRESS|COMPLETED|CANCELLED|NO_SHOW"
        int ratingGiven
    }

    event_bookings {
        UUID id PK
        string bookingRef UK "SE-XXXXXXXX"
        UUID chefId FK "nullable"
        UUID customerId
        string eventType
        date eventDate
        int guestCount
        long totalFoodPaise
        long decorationPaise
        long cakePaise
        long staffPaise
        long totalAmountPaise
        long advanceAmountPaise "50 pct"
        long platformFeePaise "15 pct"
        enum status "INQUIRY|QUOTED|CONFIRMED|ADVANCE_PAID|IN_PROGRESS|COMPLETED|CANCELLED"
    }

    chef_subscriptions {
        UUID id PK
        string subscriptionRef UK "SS-XXXXXXXX"
        UUID chefId FK
        UUID customerId
        string plan
        int mealsPerDay
        long monthlyRatePaise
        long platformFeePaise "10 pct"
        date startDate
        date endDate
        enum status "ACTIVE|PAUSED|CANCELLED|EXPIRED"
    }

    chef_menus {
        UUID id PK
        UUID chefId FK
        string name
        enum serviceType
        enum cuisineType "22 types"
        long pricePerPlatePaise
        boolean isVeg
    }

    menu_items {
        UUID id PK
        UUID menuId FK
        string name
        string category
        boolean isVeg
    }

    chef_availability {
        UUID id PK
        UUID chefId FK
        date blockedDate
        string reason
    }

    chef_photos {
        UUID id PK
        UUID chefId FK
        string url
        string photoType "FOOD|PROFILE|KITCHEN"
    }

    chef_event_pricing {
        UUID id PK
        UUID chefId FK
        string itemKey
        long customPricePaise
    }

    event_pricing_defaults {
        UUID id PK
        string category "BASE_CONFIG|LIVE_COUNTER|ADDON"
        string itemKey UK
        long defaultPricePaise
        string priceType "FIXED|PER_PLATE|PER_PERSON|PERCENTAGE"
    }

    cuisine_price_tiers {
        UUID id PK
        UUID chefId FK
        string cuisineType
        long pricePerPlatePaise
    }

    chef_referrals {
        UUID id PK
        UUID referrerId FK
        UUID referredChefId
        long bonusPaise "Rs 500"
        string status "PENDING|PAID"
    }
```

## 3. Chef Booking State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT: Customer creates booking
    PENDING_PAYMENT --> PENDING: confirmPayment (10% advance via Razorpay)
    PENDING_PAYMENT --> CANCELLED: Auto-expire after 2hrs
    PENDING --> CONFIRMED: Chef confirms
    PENDING --> CANCELLED: Cancel (customer/chef)
    CONFIRMED --> IN_PROGRESS: Chef updates location
    CONFIRMED --> CANCELLED: Cancel (customer/chef)
    IN_PROGRESS --> COMPLETED: Chef marks complete
    IN_PROGRESS --> CANCELLED: Cancel
    COMPLETED --> [*]: Rate (1-5 stars)
    CANCELLED --> [*]

    note right of PENDING_PAYMENT
        Scheduler checks every 5 min
        Auto-cancels after 2 hours
    end note

    note right of CONFIRMED
        Reminder sent at 6 PM
        day before service
    end note
```

## 4. Event Booking State Machine

```mermaid
stateDiagram-v2
    [*] --> INQUIRY: Customer creates (chefId optional)
    INQUIRY --> QUOTED: Chef quotes price (auto-assigns if unassigned)
    INQUIRY --> QUOTED: Admin assigns chef
    INQUIRY --> CANCELLED: Cancel
    QUOTED --> CONFIRMED: Customer confirms quote
    QUOTED --> INQUIRY: Customer modifies (re-quote needed)
    QUOTED --> CANCELLED: Cancel
    CONFIRMED --> ADVANCE_PAID: 50% advance paid
    CONFIRMED --> CANCELLED: Cancel
    ADVANCE_PAID --> COMPLETED: Chef completes event
    ADVANCE_PAID --> CANCELLED: Cancel
    COMPLETED --> [*]: Rate (1-5 stars)
    CANCELLED --> [*]
```

## 5. Request Flow Sequence

```mermaid
sequenceDiagram
    participant C as Customer
    participant GW as API Gateway
    participant JWT as JwtAuthFilter
    participant SC as SecurityConfig
    participant CTRL as Controller
    participant SVC as Service
    participant DB as PostgreSQL
    participant K as Kafka

    C->>GW: POST /api/v1/chef-bookings
    GW->>GW: Validate JWT
    GW->>JWT: Forward + X-User-Id header
    JWT->>JWT: Extract userId from header
    JWT->>SC: Pass to security chain
    SC->>SC: Check: POST /chef-bookings = authenticated
    SC->>CTRL: ChefBookingController.createBooking()
    CTRL->>CTRL: Extract customerId from auth
    CTRL->>SVC: createBooking(customerId, req)
    SVC->>DB: Find chef by chefId
    SVC->>SVC: Calculate pricing (15% fee, 10% advance)
    SVC->>DB: Save ChefBooking
    SVC->>K: chef.booking.created
    SVC-->>CTRL: ChefBooking entity
    CTRL-->>GW: 201 Created + JSON
    GW-->>C: Response

    Note over K: Notification service<br/>picks up event and<br/>sends confirmation<br/>email/SMS
```

## 6. Admin Cook Assignment Flow

```mermaid
sequenceDiagram
    participant A as Admin
    participant GW as API Gateway
    participant EC as EventBookingController
    participant ES as EventBookingService
    participant DB as PostgreSQL

    A->>GW: GET /api/v1/chef-events/admin/all
    GW->>EC: List all events (pageable)
    EC->>ES: browseEvents(pageable)
    ES->>DB: findAll(pageable)
    DB-->>A: Page of events (some with chefId=null)

    A->>GW: GET /api/v1/chefs/admin/all
    GW-->>A: List of verified chefs

    A->>GW: POST /api/v1/chef-events/admin/{id}/assign?chefId=xxx
    GW->>EC: adminAssignChef(id, chefId)
    EC->>ES: adminAssignChef(id, chefId)
    ES->>DB: Find event + Find chef
    ES->>ES: Set chefId, chefName
    ES->>ES: Recalculate pricing with chef's rates
    ES->>DB: Save updated event
    DB-->>A: Updated event with chef assigned
```

## 7. Component Architecture

```mermaid
graph LR
    subgraph Controllers ["Controllers (12)"]
        C1[ChefProfile]
        C2[ChefBooking]
        C3[EventBooking]
        C4[ChefSubscription]
        C5[ChefMenu]
        C6[ChefAvailability]
        C7[ChefPhoto]
        C8[EventPricing]
        C9[CuisinePricing]
        C10[ChefReferral]
        C11[Invoice]
        C12[LiveTracking]
    end

    subgraph Services ["Services (12)"]
        S1[ChefProfileService]
        S2[ChefBookingService]
        S3[EventBookingService]
        S4[ChefSubscriptionService]
        S5[ChefMenuService]
        S6[ChefAvailabilityService]
        S7[ChefPhotoService]
        S8[EventPricingService]
        S9[CuisinePricingService]
        S10[ChefReferralService]
        S11[InvoiceService]
        S12[LiveTrackingService]
    end

    subgraph Schedulers ["Scheduled Jobs (2)"]
        J1[ChefBookingScheduler<br/>5min: auto-expire<br/>6PM: reminders]
        J2[ChefBadgeService<br/>3AM: badge calc]
    end

    subgraph Repos ["Repositories (12)"]
        R1[ChefProfileRepo]
        R2[ChefBookingRepo]
        R3[EventBookingRepo]
        R4[ChefSubscriptionRepo]
        R5[ChefMenuRepo]
        R6[MenuItemRepo]
        R7[ChefAvailabilityRepo]
        R8[ChefPhotoRepo]
        R9[ChefEventPricingRepo]
        R10[EventPricingDefaultRepo]
        R11[CuisinePriceTierRepo]
        R12[ChefReferralRepo]
    end

    C1 --> S1
    C2 --> S2
    C3 --> S3
    C4 --> S4
    C5 --> S5
    C6 --> S6
    C7 --> S7
    C8 --> S8
    C9 --> S9
    C10 --> S10
    C11 --> S11
    C12 --> S12

    S1 --> R1
    S2 --> R2
    S2 --> R1
    S2 --> R5
    S3 --> R3
    S3 --> R1
    S4 --> R4
    S4 --> R1
    S5 --> R5
    S5 --> R6
    S6 --> R7
    S6 --> R2
    S7 --> R8
    S8 --> R9
    S8 --> R10
    S9 --> R11
    S9 --> R1
    S10 --> R12
    S10 --> R1
    S11 --> R2
    S11 --> R3
    S12 --> R2
    S12 --> R1
    J1 --> R2
    J2 --> R1
```
