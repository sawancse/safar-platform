-- V23: Services-leg first-class listings.
--
-- Replaces the chefs.partner_vendors directory pattern with a self-publishable
-- listing model parallel to listings.listings (stay leg). Vendor self-onboards
-- via wizard, admin clicks Approve, listing goes VERIFIED.
--
-- Architecture:
--   * service_listings — parent: shared columns for ALL service types
--   * {type}_attributes — child 1:1 (JOINED inheritance) per service_type
--   * service_items     — optional 1:N: vendor-published items (cake / pandit / decor / appliance)
--   * service_listing_tags — polymorphic facets (LANGUAGE/TRADITION/STYLE/OCCASION/RELIGION)
--   * service_listing_availability — calendar (DAY_GRAIN | SLOT_GRAIN)
--   * vendor_kyc_documents — uniform doc storage; per-type required docs enforced in code
--
-- Lifecycle: DRAFT -> PENDING_REVIEW -> VERIFIED -> PAUSED (vendor) | SUSPENDED (admin)
--
-- See: docs/prd-services-leg.md, docs/sprint-plan-services-leg.md

CREATE SCHEMA IF NOT EXISTS services;

-- ======================================================================
-- Parent: service_listings
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.service_listings (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_user_id              UUID         NOT NULL,                    -- FK to auth.users (the human who self-published)
    service_type                VARCHAR(40)  NOT NULL,                    -- discriminator: CAKE_DESIGNER, SINGER, PANDIT, DECORATOR, STAFF_HIRE, ...

    -- Identity (Primitive #4, #19)
    business_name               VARCHAR(200) NOT NULL,
    vendor_slug                 VARCHAR(100) NOT NULL,                    -- powers /services/{category}/{slug}
    hero_image_url              TEXT,
    tagline                     VARCHAR(280),
    about_md                    TEXT,
    founded_year                INT,

    -- Lifecycle (Primitive #12) — admin-load-reducer
    status                      VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',    -- DRAFT / PENDING_REVIEW / VERIFIED / PAUSED / SUSPENDED
    status_changed_at           TIMESTAMPTZ,
    status_changed_by           UUID,
    rejection_reason            TEXT,

    -- Coverage (Primitive #6) + channel (Insight #5)
    cities                      TEXT[],
    home_city                   VARCHAR(100),
    home_pincode                VARCHAR(10),
    home_address                TEXT,
    home_lat                    NUMERIC(9,6),
    home_lng                    NUMERIC(9,6),
    delivery_radius_km          INT,
    outstation_capable          BOOLEAN      NOT NULL DEFAULT FALSE,
    delivery_channels           VARCHAR(20)[],                            -- IN_PERSON, ONLINE, HYBRID

    -- Pricing (Primitive #9) — Insight #6: 4 patterns collapse N variants
    pricing_pattern             VARCHAR(30)  NOT NULL,                    -- PER_UNIT_TIERED, PER_TIME_BLOCK, FLAT_PER_ITEM, QUOTE_ON_REQUEST
    pricing_formula             JSONB,                                    -- type-specific multipliers/surcharges

    -- Calendar discriminator (Primitive #7) — Insight #7
    calendar_mode               VARCHAR(20),                              -- DAY_GRAIN | SLOT_GRAIN
    default_lead_time_hours     INT,

    -- Cancellation policy (Primitive #13)
    cancellation_policy         VARCHAR(30),                              -- FLEXIBLE, MODERATE, STRICT
    cancellation_terms_md       TEXT,

    -- Aggregate ratings (Primitive #11) — denormalized for search-page perf
    avg_rating                  NUMERIC(3,2),
    rating_count                INT          NOT NULL DEFAULT 0,
    completed_bookings_count    INT          NOT NULL DEFAULT 0,

    -- Trust tier (JustDial dual-gate pattern) — recomputed nightly
    trust_tier                  VARCHAR(20)  NOT NULL DEFAULT 'LISTED',   -- LISTED | SAFAR_VERIFIED | TOP_RATED

    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_service_listings_slug UNIQUE (vendor_slug),
    CONSTRAINT chk_service_listings_status CHECK (
        status IN ('DRAFT','PENDING_REVIEW','VERIFIED','PAUSED','SUSPENDED')
    )
);

CREATE INDEX IF NOT EXISTS idx_listings_type_status     ON services.service_listings(service_type, status);
CREATE INDEX IF NOT EXISTS idx_listings_status_verified ON services.service_listings(status) WHERE status = 'VERIFIED';
CREATE INDEX IF NOT EXISTS idx_listings_vendor_user     ON services.service_listings(vendor_user_id);
CREATE INDEX IF NOT EXISTS idx_listings_home_city       ON services.service_listings(home_city) WHERE status = 'VERIFIED';

-- ======================================================================
-- Child: cake_attributes (1:1 with service_listings)
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.cake_attributes (
    service_listing_id          UUID PRIMARY KEY REFERENCES services.service_listings(id) ON DELETE CASCADE,
    bakery_type                 VARCHAR(30),                              -- HOME_BAKER, COMMERCIAL, CLOUD_KITCHEN
    oven_capacity_kg_per_day    INT,
    flavours_offered            VARCHAR(40)[],
    design_styles               VARCHAR(40)[],
    max_tier_count              INT,
    eggless_capable             BOOLEAN,
    vegan_capable               BOOLEAN,
    delivery_mode               VARCHAR(20)                               -- SELF, PARTNER, PICKUP_ONLY
);

-- ======================================================================
-- Child: singer_attributes
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.singer_attributes (
    service_listing_id          UUID PRIMARY KEY REFERENCES services.service_listings(id) ON DELETE CASCADE,
    act_type                    VARCHAR(20),                              -- SOLO, DUO, BAND, TROUPE
    genres                      VARCHAR(40)[],
    languages                   VARCHAR(40)[],
    troupe_size_min             INT,
    troupe_size_max             INT,
    religious_capable           BOOLEAN,
    audio_reels                 TEXT[],                                   -- S3 URLs
    video_reels                 TEXT[],
    equipment_owned             VARCHAR(20),                              -- FULL_PA, PARTIAL, NONE
    setup_time_minutes          INT
);

-- ======================================================================
-- Child: pandit_attributes
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.pandit_attributes (
    service_listing_id          UUID PRIMARY KEY REFERENCES services.service_listings(id) ON DELETE CASCADE,
    tradition                   VARCHAR(40),                              -- SMARTA, VAISHNAV, SHAIVITE, SINDHI, ARYA_SAMAJ, IYER, IYENGAR, MAITHIL, ...
    pandit_gotra                VARCHAR(60),
    text_languages              VARCHAR(40)[],
    shastra_specializations     VARCHAR(40)[],                            -- KARMA_KANDA, JYOTISH, VEDIC, TANTRIC, ASTROLOGY
    puja_types_offered          VARCHAR(40)[],                            -- GRIHA_PRAVESH, SATYANARAYAN, WEDDING, MUNDAN, ...
    samagri_provided            VARCHAR(20),                              -- ALL, PARTIAL, NONE
    online_via_video_call       BOOLEAN
);

-- ======================================================================
-- Child: decor_attributes
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.decor_attributes (
    service_listing_id          UUID PRIMARY KEY REFERENCES services.service_listings(id) ON DELETE CASCADE,
    decor_styles                VARCHAR(40)[],                            -- PUNJABI, SOUTH_INDIAN, MARWARI, BENGALI, MODERN, RUSTIC, FUSION, THEME, MINIMALIST, ROYAL
    setup_capabilities          VARCHAR(40)[],                            -- FLORAL, LIGHTING, STAGE, MANDAP, SEATING, CENTERPIECES, BACKDROP, PROPS
    indoor_capable              BOOLEAN,
    outdoor_capable             BOOLEAN,
    largest_event_handled_pax   INT,
    crew_size_default           INT,
    equipment_owned             TEXT[]                                    -- free-form: 'generator','lighting_rig','transport_vehicle',...
);

-- ======================================================================
-- Child: staff_attributes
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.staff_attributes (
    service_listing_id          UUID PRIMARY KEY REFERENCES services.service_listings(id) ON DELETE CASCADE,
    agency_type                 VARCHAR(30),                              -- INDIVIDUAL_FREELANCER, SMALL_AGENCY, LARGE_AGENCY
    roles_offered               VARCHAR(40)[],                            -- WAITER, BARTENDER, SERVER, KITCHEN_HELPER, HOSTESS, VALET, SECURITY, CLEANER, ...
    min_count_per_booking       INT,
    max_count_per_booking       INT,
    uniform_provided            BOOLEAN,
    experience_years_avg        INT,
    languages_spoken            VARCHAR(40)[],
    dress_codes_supported       VARCHAR(40)[]                             -- FORMAL_BLACK, SAREE, KURTA, EVENT_THEME
);

-- ======================================================================
-- service_items — optional 1:N (catalog-driven types: cake, decor, pandit, appliance)
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.service_items (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_listing_id          UUID         NOT NULL REFERENCES services.service_listings(id) ON DELETE CASCADE,

    title                       VARCHAR(200) NOT NULL,                    -- "3-tier Chocolate Truffle Birthday Cake"
    hero_photo_url              TEXT,
    photos                      TEXT[],
    description_md              TEXT,
    base_price_paise            BIGINT       NOT NULL,
    options_json                JSONB,                                    -- type-specific options: {weight_options, tier_options, flavour_options, ...}

    occasion_tags               VARCHAR(40)[],                            -- BIRTHDAY, WEDDING, ANNIVERSARY, CORPORATE, RELIGIOUS, ...
    lead_time_hours             INT,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | PAUSED
    display_order               INT          NOT NULL DEFAULT 0,

    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_service_items_status CHECK (status IN ('ACTIVE','PAUSED'))
);

CREATE INDEX IF NOT EXISTS idx_service_items_listing ON services.service_items(service_listing_id, status);
CREATE INDEX IF NOT EXISTS idx_service_items_occasion ON services.service_items USING GIN (occasion_tags);

-- ======================================================================
-- service_listing_tags — polymorphic cross-cutting facets (Insight #4)
--   Powers cross-type discovery: "planning a Punjabi wedding" -> pandit + singer + cook + decor
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.service_listing_tags (
    service_listing_id          UUID         NOT NULL REFERENCES services.service_listings(id) ON DELETE CASCADE,
    tag_namespace               VARCHAR(20)  NOT NULL,                    -- LANGUAGE, TRADITION, STYLE, OCCASION, RELIGION
    tag_value                   VARCHAR(60)  NOT NULL,
    PRIMARY KEY (service_listing_id, tag_namespace, tag_value)
);

CREATE INDEX IF NOT EXISTS idx_listing_tags_value ON services.service_listing_tags(tag_namespace, tag_value);

-- ======================================================================
-- service_listing_availability — calendar (Insight #7)
--   DAY_GRAIN  -> start_time and end_time NULL; full date booked
--   SLOT_GRAIN -> start_time and end_time set; specific window
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.service_listing_availability (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_listing_id          UUID         NOT NULL REFERENCES services.service_listings(id) ON DELETE CASCADE,
    date                        DATE         NOT NULL,
    start_time                  TIME,                                     -- NULL for DAY_GRAIN
    end_time                    TIME,                                     -- NULL for DAY_GRAIN
    status                      VARCHAR(20)  NOT NULL,                    -- AVAILABLE, BOOKED, BLACKOUT, HIGH_DEMAND
    booking_id                  UUID,                                     -- if BOOKED
    notes                       TEXT,

    CONSTRAINT chk_avail_status CHECK (status IN ('AVAILABLE','BOOKED','BLACKOUT','HIGH_DEMAND'))
);

CREATE INDEX IF NOT EXISTS idx_availability_listing_date ON services.service_listing_availability(service_listing_id, date);
CREATE INDEX IF NOT EXISTS idx_availability_date_status  ON services.service_listing_availability(date, status) WHERE status = 'AVAILABLE';

-- ======================================================================
-- vendor_kyc_documents — uniform doc storage; per-type requirements enforced in code
--   ServiceListingPublishValidator reads KYC_GATES_BY_TYPE config and refuses
--   DRAFT -> PENDING_REVIEW transition unless required docs are present.
-- ======================================================================

CREATE TABLE IF NOT EXISTS services.vendor_kyc_documents (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_listing_id          UUID         NOT NULL REFERENCES services.service_listings(id) ON DELETE CASCADE,
    document_type               VARCHAR(40)  NOT NULL,                    -- AADHAAR, PAN, FSSAI, IPRS, POLICE_VERIFICATION, GST, LINEAGE_PROOF, INSURANCE
    document_url                TEXT         NOT NULL,
    document_number             VARCHAR(50),
    verification_status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING, VERIFIED, REJECTED
    verified_at                 TIMESTAMPTZ,
    verified_by                 UUID,
    expires_at                  DATE,
    rejection_reason            TEXT,

    uploaded_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_kyc_verification CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_kyc_listing      ON services.vendor_kyc_documents(service_listing_id);
CREATE INDEX IF NOT EXISTS idx_kyc_listing_type ON services.vendor_kyc_documents(service_listing_id, document_type);
