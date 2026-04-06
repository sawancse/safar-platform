# Chef Service Architecture

## 1. High-Level Service Architecture

```
+------------------------------------------------------------------+
|                        API GATEWAY (:8080)                        |
|  /api/v1/chefs/**  /api/v1/chef-bookings/**  /api/v1/chef-events/**  |
|  /api/v1/chef-subscriptions/**  /api/v1/chef-events/pricing/**   |
+----------------------------------+-------------------------------+
                                   |
                          JWT + X-User-Id
                                   |
                                   v
+------------------------------------------------------------------+
|                     CHEF SERVICE (:8093)                          |
|                                                                  |
|  +------------------+  +-------------------+  +----------------+ |
|  | JwtAuthFilter    |  | SecurityConfig    |  | GlobalException| |
|  | (validates JWT)  |  | (route security)  |  | Handler (7807) | |
|  +------------------+  +-------------------+  +----------------+ |
|                                                                  |
|  +-----------+ +-----------+ +-----------+ +-----------+         |
|  |  Chef     | |  Booking  | |  Event    | | Subscript | ...8    |
|  | Profile   | | Controller| | Controller| | Controller| more   |
|  | Controller| |           | |           | |           |         |
|  +-----------+ +-----------+ +-----------+ +-----------+         |
|       |              |             |             |               |
|  +-----------+ +-----------+ +-----------+ +-----------+         |
|  |  Chef     | |  Booking  | |  Event    | | Subscript |         |
|  | Profile   | | Service   | | Booking   | | Service   |         |
|  | Service   | |           | | Service   | |           |         |
|  +-----------+ +-----------+ +-----------+ +-----------+         |
|       |              |             |             |               |
|  +-----------+ +-----------+ +-----------+ +-----------+         |
|  |  Chef     | |  Booking  | |  Event    | | Subscript |         |
|  | Profile   | | Repository| | Booking   | | Repository|         |
|  | Repository| |           | | Repository| |           |         |
|  +-----------+ +-----------+ +-----------+ +-----------+         |
|                           |                                      |
+------------------------------------------------------------------+
                            |
              +-------------+-------------+
              |                           |
              v                           v
   +-------------------+      +---------------------+
   |   PostgreSQL       |      |   Apache Kafka      |
   |   Schema: chefs    |      |   (MSK Serverless)  |
   |   11 tables        |      |   7 topics          |
   +-------------------+      +---------------------+
```

## 2. Entity Relationship Diagram

```
+========================+          +========================+
|    chef_profiles       |          |    chef_menus          |
|========================|          |========================|
| id (PK, UUID)         |<----+    | id (PK, UUID)         |
| userId (unique)       |     |    | chefId (FK) ----------|---+
| name, phone, email    |     |    | name, description     |
| chefType (enum)       |     |    | serviceType (enum)    |
| city, state, pincode  |     |    | cuisineType (enum)    |
| cuisines, specialties |     |    | mealType (enum)       |
| dailyRatePaise        |     |    | pricePerPlatePaise    |
| monthlyRatePaise      |     |    | isVeg, isVegan, isJain|
| eventMinPlatePaise    |     |    | photoUrl, active      |
| rating, reviewCount   |     |    +========================+
| totalBookings         |     |              |
| verificationStatus    |     |              | 1:N
| available, verified   |     |              v
| badge, badgeAwardedAt |     |    +========================+
| referralCode (unique) |     |    |    menu_items          |
| bankAccount fields    |     |    |========================|
+========================+     |    | id (PK, UUID)         |
    |  |  |  |  |             |    | menuId (FK)           |
    |  |  |  |  |             |    | name, description     |
    |  |  |  |  |             |    | category, isVeg       |
    |  |  |  |  |             |    | sortOrder             |
    |  |  |  |  |             |    +========================+
    |  |  |  |  |             |
    |  |  |  |  +-------------|-----> chef_availability
    |  |  |  |                |       (chefId, blockedDate, reason)
    |  |  |  |                |
    |  |  |  +----------------|-----> chef_photos
    |  |  |                   |       (chefId, url, caption, photoType)
    |  |  |                   |
    |  |  +-------------------|-----> chef_event_pricing
    |  |                      |       (chefId, itemKey, customPricePaise)
    |  |                      |
    |  +----------------------|-----> cuisine_price_tiers
    |                         |       (chefId, cuisineType, pricePerPlatePaise)
    |                         |
    +-------------------------|-----> chef_referrals
                              |       (referrerId, referredChefId, bonusPaise)
                              |
    +========================+|     +========================+
    |   chef_bookings        ||     |   event_bookings       |
    |========================||     |========================|
    | id (PK, UUID)          ||     | id (PK, UUID)         |
    | bookingRef (SC-XXXX)   ||     | bookingRef (SE-XXXX)  |
    | chefId (FK) -----------+|     | chefId (FK, nullable)-+
    | customerId             ||     | customerId            |
    | serviceType, mealType  ||     | eventType, eventDate  |
    | serviceDate, serviceTime|     | durationHours         |
    | guestsCount, menuId    ||     | guestCount            |
    | totalAmountPaise       ||     | venueAddress, city    |
    | advanceAmountPaise(10%)||     | totalFoodPaise        |
    | platformFeePaise (15%) ||     | decoration/cake/staff |
    | razorpay fields        ||     | totalAmountPaise      |
    | status (7 states)      ||     | advanceAmountPaise(50%)|
    | tracking (lat/lng/eta) ||     | platformFeePaise (15%)|
    | ratingGiven, review    ||     | status (7 states)     |
    +========================+|     | ratingGiven, review   |
                              |     +========================+
    +========================+|
    |  chef_subscriptions    ||     +========================+
    |========================||     | event_pricing_defaults |
    | id (PK, UUID)          ||     |========================|
    | subscriptionRef(SS-XXX)||     | id (PK, UUID)         |
    | chefId (FK) -----------+      | category, itemKey     |
    | customerId             |      | label, description    |
    | plan, mealsPerDay      |      | defaultPricePaise     |
    | monthlyRatePaise       |      | priceType (FIXED/     |
    | platformFeePaise (10%) |      |   PER_PLATE/PER_PERSON|
    | startDate, endDate     |      |   /PERCENTAGE)        |
    | status (4 states)      |      | minPrice, maxPrice    |
    +========================+      +========================+
```

## 3. Booking Lifecycle State Machines

### Chef Booking (Daily/One-Time)
```
                    +------------------+
                    | PENDING_PAYMENT  |  (created, awaiting Razorpay)
                    +--------+---------+
                             |
                    confirmPayment (10% advance)
                             |
              +----- 2h -----+
              | auto-expire  v
              |     +--------+---------+
              +---->|    CANCELLED     |<-----+-----+-----+
                    +------------------+      |     |     |
                             |                |     |     |
                             v                |     |     |
                    +--------+---------+      |     |     |
                    |     PENDING      |------+     |     |
                    +--------+---------+  cancel    |     |
                             |                      |     |
                    chef.confirm()                  |     |
                             |                      |     |
                    +--------+---------+            |     |
                    |    CONFIRMED     |------------+     |
                    +--------+---------+  cancel          |
                             |                            |
                    location.update() (auto)              |
                             |                            |
                    +--------+---------+                  |
                    |   IN_PROGRESS    |------------------+
                    +--------+---------+  cancel
                             |
                    chef.complete()
                             |
                    +--------+---------+
                    |    COMPLETED     |  --> rate(1-5 stars)
                    +------------------+
```

### Event Booking (Party/Corporate)
```
                    +------------------+
                    |     INQUIRY      |  (chefId optional)
                    +--------+---------+
                             |
                    chef.quote(amount)  [assigns chef if unassigned]
                    admin.assign(chefId)
                             |
                    +--------+---------+        +-----------+
                    |     QUOTED       |------->| CANCELLED |
                    +--------+---------+        +-----------+
                             |                       ^
                    customer.confirm()               |
                             |                       |
                    +--------+---------+             |
                    |    CONFIRMED     |-------------+
                    +--------+---------+
                             |
                    markAdvancePaid (50%)
                             |
                    +--------+---------+
                    |   ADVANCE_PAID   |-------------+
                    +--------+---------+             |
                             |                       v
                    chef.complete()            +-----------+
                             |                | CANCELLED  |
                    +--------+---------+      +-----------+
                    |    COMPLETED     |  --> rate(1-5 stars)
                    +------------------+
```

### Subscription Lifecycle
```
        +------------------+
        |     ACTIVE       |  (30-day cycles)
        +--------+---------+
                 |        \
          cancel  \        \ pause (future)
                 |  \       \
        +--------v-+ +------v-----+      +----------+
        | CANCELLED | |   PAUSED   |      | EXPIRED  |
        +-----------+ +------------+      +----------+
```

## 4. API Endpoint Map

```
/api/v1/chefs
  |
  +-- GET    /                              [PUBLIC]  Browse chefs (city, pageable)
  +-- POST   /                              [AUTH]    Register chef
  +-- GET    /search                        [PUBLIC]  Search (city,cuisine,locality...)
  +-- GET    /me                            [AUTH]    My profile
  +-- PUT    /me                            [AUTH]    Update profile
  +-- PUT    /me/availability               [AUTH]    Toggle availability
  +-- GET    /{chefId}                      [PUBLIC]  Get chef
  |
  +-- /admin
  |     +-- GET  /pending                   [ADMIN]   Pending chefs
  |     +-- GET  /all                       [ADMIN]   All chefs
  |     +-- POST /{chefId}/verify           [ADMIN]   Verify
  |     +-- POST /{chefId}/reject           [ADMIN]   Reject
  |     +-- POST /{chefId}/suspend          [ADMIN]   Suspend
  |
  +-- /availability
  |     +-- POST  /block                    [AUTH]    Block date
  |     +-- DELETE /unblock                 [AUTH]    Unblock date
  |     +-- POST  /block-bulk               [AUTH]    Bulk block
  |     +-- GET   /{chefId}/calendar        [PUBLIC]  Calendar view
  |
  +-- /photos
  |     +-- GET   /{chefId}                 [PUBLIC]  Chef photos
  |     +-- POST  /                         [AUTH]    Add photo
  |     +-- DELETE /{photoId}               [AUTH]    Delete photo
  |
  +-- /{chefId}/menus
  |     +-- GET   /                         [PUBLIC]  Get menus
  |     +-- POST  /                         [AUTH]    Create menu
  |     +-- DELETE /menus/{menuId}          [AUTH]    Delete menu
  |     +-- GET   /menus/{menuId}/items     [PUBLIC]  Menu items
  |
  +-- /cuisine-pricing
  |     +-- GET   /{chefId}                 [PUBLIC]  Cuisine pricing
  |     +-- PUT   /{cuisineType}            [AUTH]    Set price
  |     +-- DELETE /{cuisineType}           [AUTH]    Delete price
  |
  +-- /referrals
        +-- POST  /generate-code            [AUTH]    Generate code
        +-- GET   /my                       [AUTH]    My referrals

/api/v1/chef-bookings
  |
  +-- POST   /                              [AUTH]    Create booking
  +-- GET    /{id}                          [AUTH]    Get booking
  +-- POST   /{id}/confirm-payment          [AUTH]    Confirm Razorpay payment
  +-- POST   /{id}/confirm                  [AUTH]    Chef confirms
  +-- POST   /{id}/cancel                   [AUTH]    Cancel
  +-- POST   /{id}/complete                 [AUTH]    Chef completes
  +-- POST   /{id}/rate                     [AUTH]    Rate (1-5)
  +-- PUT    /{id}                          [AUTH]    Modify booking
  +-- POST   /{id}/rebook                   [AUTH]    Rebook
  +-- GET    /my                            [AUTH]    My bookings
  +-- GET    /chef                          [AUTH]    Chef's bookings
  +-- GET    /{id}/tracking                 [PUBLIC]  Live tracking
  +-- POST   /{id}/location                 [AUTH]    Update location
  +-- GET    /{id}/invoice                  [PUBLIC]  Invoice
  |
  +-- /admin
        +-- GET  /all                       [ADMIN]   All bookings
        +-- POST /{id}/assign?chefId=       [ADMIN]   Assign cook

/api/v1/chef-events
  |
  +-- GET    /                              [PUBLIC]  Browse events
  +-- POST   /                              [PUBLIC]  Create inquiry
  +-- GET    /{id}                          [PUBLIC]  Get event
  +-- POST   /{id}/quote                   [AUTH]    Chef quotes
  +-- POST   /{id}/confirm                 [AUTH]    Customer confirms
  +-- POST   /{id}/advance-paid            [AUTH]    Mark advance paid
  +-- POST   /{id}/complete                [AUTH]    Chef completes
  +-- POST   /{id}/cancel                  [AUTH]    Cancel
  +-- POST   /{id}/rate                    [AUTH]    Rate (1-5)
  +-- PUT    /{id}                          [AUTH]    Modify
  +-- GET    /my                            [AUTH]    My events
  +-- GET    /chef                          [AUTH]    Chef's events
  +-- GET    /{id}/invoice                  [PUBLIC]  Invoice
  |
  +-- /pricing
  |     +-- GET   /                         [PUBLIC]  Get pricing
  |     +-- GET   /me                       [AUTH]    My pricing
  |     +-- PUT   /me                       [AUTH]    Update bulk
  |     +-- PUT   /me/{itemKey}             [AUTH]    Update single
  |     +-- DELETE /me/{itemKey}            [AUTH]    Reset to default
  |     +-- GET   /admin                    [ADMIN]   All defaults
  |     +-- POST  /admin                    [ADMIN]   Create default
  |     +-- PUT   /admin/{itemKey}          [ADMIN]   Update default
  |     +-- DELETE /admin/{itemKey}         [ADMIN]   Deactivate
  |
  +-- /admin
        +-- GET  /all                       [ADMIN]   All events
        +-- POST /{id}/assign?chefId=       [ADMIN]   Assign cook

/api/v1/chef-subscriptions
  |
  +-- POST   /                              [AUTH]    Create subscription
  +-- GET    /{id}                          [AUTH]    Get subscription
  +-- POST   /{id}/cancel                  [AUTH]    Cancel
  +-- PUT    /{id}                          [AUTH]    Modify
  +-- GET    /my                            [AUTH]    My subscriptions
  +-- GET    /chef                          [AUTH]    Chef's subscriptions
  |
  +-- /admin
        +-- GET  /all                       [ADMIN]   All subscriptions
```

## 5. Kafka Event Flow

```
+-------------------+                              +----------------------+
|   Chef Service    |                              | Notification Service |
|   (Producer)      |                              |    (Consumer)        |
+-------------------+                              +----------------------+
        |                                                    ^
        |  chef.booking.created --------------------------->|
        |  chef.booking.payment.confirmed ----------------->|
        |  chef.booking.confirmed ------------------------->|
        |  chef.booking.cancelled ------------------------->|
        |  chef.booking.completed ------------------------->|
        |  chef.booking.modified -------------------------->|
        |  chef.booking.reminder -------------------------->|
        |                                                    |
        +----------------------------------------------------+

Event Payload (JSON):
{
  "bookingId": "uuid",
  "bookingRef": "SC-XXXXXXXX",
  "chefId": "uuid",
  "customerId": "uuid",
  "chefName": "...",
  "customerName": "...",
  "serviceDate": "2026-04-10",
  "mealType": "LUNCH",
  "status": "CONFIRMED",
  "totalAmountPaise": 50000,
  "advanceAmountPaise": 5000,
  "balanceAmountPaise": 45000,
  "paymentStatus": "ADVANCE_PAID",
  "city": "Hyderabad"
}
```

## 6. Pricing Model

```
+================================================================+
|                    CHEF BOOKING (Daily)                         |
|================================================================|
|  Total = (menuPrice OR dailyRate) x guests x meals             |
|  Platform Fee = 15% of total                                   |
|  Chef Earnings = 85% of total                                  |
|  Advance = max(10% of total, Rs.1)                             |
|  Balance = total - advance                                     |
+================================================================+

+================================================================+
|                    EVENT BOOKING                                |
|================================================================|
|  Food Cost  = pricePerPlate x guestCount                       |
|  Decoration = Rs.5,000 (if requested)                          |
|  Cake       = Rs.2,000 (if requested)                          |
|  Staff      = Rs.1,500 x staffCount                            |
|  --------------------------------                              |
|  Total      = food + decoration + cake + staff + other         |
|  Platform Fee  = 15% of total                                  |
|  Chef Earnings = 85% of total                                  |
|  Advance       = 50% of total                                  |
|  Balance       = 50% of total                                  |
+================================================================+

+================================================================+
|                    SUBSCRIPTION (Monthly)                       |
|================================================================|
|  Monthly Rate = chef's monthlyRatePaise (or custom)            |
|  Platform Fee  = 10% of monthly rate                           |
|  Chef Earnings = 90% of monthly rate                           |
|  Duration      = 30 days from startDate                        |
+================================================================+
```

## 7. Scheduled Jobs

```
+---------------------------+-------------------+---------------------------+
|         Job               |    Schedule       |       Action              |
+---------------------------+-------------------+---------------------------+
| autoExpireUnpaidBookings  | Every 5 minutes   | Cancel PENDING_PAYMENT    |
|                           |                   | bookings older than 2hrs  |
|                           |                   | + Kafka cancel event      |
+---------------------------+-------------------+---------------------------+
| sendBookingReminders      | Daily 6:00 PM     | Send reminder for         |
|                           |                   | tomorrow's CONFIRMED      |
|                           |                   | bookings via Kafka        |
+---------------------------+-------------------+---------------------------+
| recalculateBadges         | Daily 3:00 AM     | Evaluate all chefs for    |
|                           |                   | TOP_CHEF, TOP_10,         |
|                           |                   | RISING_STAR, VERIFIED_PRO |
+---------------------------+-------------------+---------------------------+
```

## 8. Badge Criteria

```
+------------------+----------------------------------------------+
|     Badge        |              Criteria                        |
+------------------+----------------------------------------------+
| TOP_CHEF         | rating >= 4.8, bookings >= 50, comp >= 95%  |
| TOP_10           | Top 10 by rating (min 20 bookings)          |
| RISING_STAR      | rating >= 4.5, bookings >= 10, comp >= 90%  |
| VERIFIED_PRO     | verified + foodSafetyCertificate             |
+------------------+----------------------------------------------+
```

## 9. Database Tables Summary

```
Schema: chefs (PostgreSQL)
+---------------------------+----------+----------------------------------+
|        Table              | Records  | Purpose                          |
+---------------------------+----------+----------------------------------+
| chef_profiles             | core     | Chef identity & settings         |
| chef_menus                | core     | Menu packages per chef           |
| menu_items                | core     | Items within a menu              |
| chef_bookings             | txn      | Daily/one-time cook bookings     |
| event_bookings            | txn      | Party/corporate event bookings   |
| chef_subscriptions        | txn      | Monthly cook subscriptions       |
| chef_availability         | config   | Blocked dates per chef           |
| chef_photos               | media    | Chef gallery (max 20)            |
| chef_event_pricing        | config   | Chef-specific event pricing      |
| event_pricing_defaults    | config   | Platform default event pricing   |
| cuisine_price_tiers       | config   | Per-cuisine plate pricing        |
| chef_referrals            | txn      | Referral tracking (Rs.500 bonus) |
+---------------------------+----------+----------------------------------+
Migrations: V1 through V6
```

## 10. Security Model

```
                    Request Flow
                    ============

  Client --> API Gateway --> JwtAuthFilter --> SecurityConfig --> Controller
                |                  |                |
                |            Extracts userId    Checks path:
                |            from X-User-Id     - /actuator/** = permitAll
                |            header             - GET /chefs/** = permitAll
                |                               - /admin/** = authenticated
                |                               - POST /bookings = authenticated
                |                               - Everything else = authenticated
                |
           JwtAuthFilter at Gateway
           validates JWT, sets:
           - X-User-Id header
           - X-User-Role header

  Authorization in Service Layer:
  +----------------------------------------------------------+
  | Chef operations:  chefProfile.userId == X-User-Id        |
  | Customer ops:     booking.customerId == X-User-Id        |
  | Admin ops:        authenticated (role check at gateway)  |
  | Public ops:       no auth required                       |
  +----------------------------------------------------------+
```
