CREATE SCHEMA IF NOT EXISTS chefs;

-- ── Chef Profiles ────────────────────────────────────────────────────────────
CREATE TABLE chefs.chef_profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID UNIQUE NOT NULL,
    name                    VARCHAR(200) NOT NULL,
    phone                   VARCHAR(20),
    email                   VARCHAR(200),
    bio                     TEXT,
    profile_photo_url       VARCHAR(500),

    chef_type               VARCHAR(30) NOT NULL DEFAULT 'DOMESTIC',
    experience_years        INTEGER DEFAULT 0,
    city                    VARCHAR(100),
    state                   VARCHAR(100),
    pincode                 VARCHAR(10),

    cuisines                TEXT,
    specialties             TEXT,
    localities              TEXT,

    daily_rate_paise        BIGINT DEFAULT 0,
    monthly_rate_paise      BIGINT DEFAULT 0,
    event_min_plate_paise   BIGINT DEFAULT 0,

    min_guests              INTEGER DEFAULT 1,
    max_guests              INTEGER DEFAULT 100,
    event_min_pax           INTEGER DEFAULT 10,
    event_max_pax           INTEGER DEFAULT 500,

    languages               VARCHAR(500),

    verified                BOOLEAN DEFAULT FALSE,
    verification_status     VARCHAR(20) DEFAULT 'PENDING',
    id_proof_type           VARCHAR(50),
    id_proof_number         VARCHAR(50),
    food_safety_certificate BOOLEAN DEFAULT FALSE,

    rating                  DOUBLE PRECISION DEFAULT 0.0,
    review_count            INTEGER DEFAULT 0,
    total_bookings          INTEGER DEFAULT 0,
    completion_rate         DOUBLE PRECISION DEFAULT 100.0,

    available               BOOLEAN DEFAULT TRUE,

    bank_account_name       VARCHAR(200),
    bank_account_number     VARCHAR(50),
    bank_ifsc               VARCHAR(20),

    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_chef_city ON chefs.chef_profiles(city);
CREATE INDEX idx_chef_user ON chefs.chef_profiles(user_id);
CREATE INDEX idx_chef_status ON chefs.chef_profiles(verification_status);

-- ── Chef Menus ───────────────────────────────────────────────────────────────
CREATE TABLE chefs.chef_menus (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chef_id                 UUID NOT NULL REFERENCES chefs.chef_profiles(id),
    name                    VARCHAR(200) NOT NULL,
    description             TEXT,

    service_type            VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    cuisine_type            VARCHAR(30),
    meal_type               VARCHAR(20),

    price_per_plate_paise   BIGINT NOT NULL DEFAULT 0,
    min_guests              INTEGER DEFAULT 1,
    max_guests              INTEGER,

    is_veg                  BOOLEAN DEFAULT FALSE,
    is_vegan                BOOLEAN DEFAULT FALSE,
    is_jain                 BOOLEAN DEFAULT FALSE,

    photo_url               VARCHAR(500),
    active                  BOOLEAN DEFAULT TRUE,

    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_menu_chef ON chefs.chef_menus(chef_id);

-- ── Menu Items ───────────────────────────────────────────────────────────────
CREATE TABLE chefs.menu_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_id                 UUID NOT NULL REFERENCES chefs.chef_menus(id) ON DELETE CASCADE,
    name                    VARCHAR(200) NOT NULL,
    description             TEXT,
    category                VARCHAR(30) DEFAULT 'MAIN',
    is_veg                  BOOLEAN DEFAULT TRUE,
    sort_order              INTEGER DEFAULT 0
);

CREATE INDEX idx_item_menu ON chefs.menu_items(menu_id);

-- ── Chef Bookings (daily / one-time) ─────────────────────────────────────────
CREATE TABLE chefs.chef_bookings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_ref             VARCHAR(20) UNIQUE NOT NULL,

    chef_id                 UUID NOT NULL,
    customer_id             UUID NOT NULL,
    chef_name               VARCHAR(200),
    customer_name           VARCHAR(200),

    service_type            VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    meal_type               VARCHAR(20),
    service_date            DATE NOT NULL,
    service_time            VARCHAR(10),
    guests_count            INTEGER DEFAULT 1,
    number_of_meals         INTEGER DEFAULT 1,

    menu_id                 UUID,
    menu_name               VARCHAR(200),
    special_requests        TEXT,

    address                 TEXT NOT NULL,
    city                    VARCHAR(100),
    locality                VARCHAR(100),
    pincode                 VARCHAR(10),

    total_amount_paise      BIGINT NOT NULL,
    platform_fee_paise      BIGINT DEFAULT 0,
    chef_earnings_paise     BIGINT DEFAULT 0,

    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cancellation_reason     TEXT,
    cancelled_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,

    rating_given            INTEGER,
    review_comment          TEXT,

    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_cb_chef ON chefs.chef_bookings(chef_id);
CREATE INDEX idx_cb_customer ON chefs.chef_bookings(customer_id);
CREATE INDEX idx_cb_date ON chefs.chef_bookings(service_date);
CREATE INDEX idx_cb_status ON chefs.chef_bookings(status);

-- ── Chef Subscriptions (monthly) ─────────────────────────────────────────────
CREATE TABLE chefs.chef_subscriptions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_ref        VARCHAR(20) UNIQUE NOT NULL,

    chef_id                 UUID NOT NULL,
    customer_id             UUID NOT NULL,
    chef_name               VARCHAR(200),
    customer_name           VARCHAR(200),

    plan                    VARCHAR(20) NOT NULL DEFAULT 'ONE_MEAL',
    meals_per_day           INTEGER DEFAULT 1,
    meal_types              VARCHAR(100),
    schedule                TEXT,

    monthly_rate_paise      BIGINT NOT NULL,
    platform_fee_paise      BIGINT DEFAULT 0,
    chef_earnings_paise     BIGINT DEFAULT 0,

    start_date              DATE NOT NULL,
    end_date                DATE,
    next_renewal_date       DATE,

    address                 TEXT NOT NULL,
    city                    VARCHAR(100),
    locality                VARCHAR(100),
    pincode                 VARCHAR(10),

    special_requests        TEXT,
    dietary_preferences     VARCHAR(200),
    razorpay_subscription_id VARCHAR(100),

    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    cancellation_reason     TEXT,
    cancelled_at            TIMESTAMPTZ,

    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_cs_chef ON chefs.chef_subscriptions(chef_id);
CREATE INDEX idx_cs_customer ON chefs.chef_subscriptions(customer_id);
CREATE INDEX idx_cs_status ON chefs.chef_subscriptions(status);

-- ── Event Bookings ───────────────────────────────────────────────────────────
CREATE TABLE chefs.event_bookings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_ref             VARCHAR(20) UNIQUE NOT NULL,

    chef_id                 UUID NOT NULL,
    customer_id             UUID NOT NULL,
    chef_name               VARCHAR(200),
    customer_name           VARCHAR(200),
    customer_phone          VARCHAR(20),
    customer_email          VARCHAR(200),

    event_type              VARCHAR(30) NOT NULL DEFAULT 'BIRTHDAY',
    event_date              DATE NOT NULL,
    event_time              VARCHAR(10),
    duration_hours          INTEGER DEFAULT 4,
    guest_count             INTEGER NOT NULL,

    venue_address           TEXT NOT NULL,
    city                    VARCHAR(100),
    locality                VARCHAR(100),
    pincode                 VARCHAR(10),

    menu_package_id         UUID,
    menu_description        TEXT,
    cuisine_preferences     VARCHAR(200),

    price_per_plate_paise   BIGINT DEFAULT 0,
    total_food_paise        BIGINT DEFAULT 0,
    decoration_paise        BIGINT DEFAULT 0,
    cake_paise              BIGINT DEFAULT 0,
    staff_paise             BIGINT DEFAULT 0,
    other_addons_paise      BIGINT DEFAULT 0,

    total_amount_paise      BIGINT NOT NULL DEFAULT 0,
    advance_amount_paise    BIGINT DEFAULT 0,
    balance_amount_paise    BIGINT DEFAULT 0,

    platform_fee_paise      BIGINT DEFAULT 0,
    chef_earnings_paise     BIGINT DEFAULT 0,

    addons_json             TEXT,
    special_requests        TEXT,

    status                  VARCHAR(20) NOT NULL DEFAULT 'INQUIRY',
    quoted_at               TIMESTAMPTZ,
    confirmed_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     TEXT,

    rating_given            INTEGER,
    review_comment          TEXT,

    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_eb_chef ON chefs.event_bookings(chef_id);
CREATE INDEX idx_eb_customer ON chefs.event_bookings(customer_id);
CREATE INDEX idx_eb_date ON chefs.event_bookings(event_date);
CREATE INDEX idx_eb_status ON chefs.event_bookings(status);
