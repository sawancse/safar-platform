-- Safar Listing Service Schema
-- V1: Initial schema creation

CREATE SCHEMA IF NOT EXISTS listings;

CREATE TABLE listings.listings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id                 UUID NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    description             TEXT NOT NULL,
    type                    VARCHAR(20) NOT NULL
                                CHECK (type IN ('HOME','ROOM','UNIQUE','COMMERCIAL')),
    commercial_category     VARCHAR(30)
                                CHECK (commercial_category IN (
                                    'MEETING_ROOM','EVENT_VENUE','PHOTO_STUDIO',
                                    'PODCAST_STUDIO','COMMERCIAL_KITCHEN',
                                    'COWORKING_SPACE','TRAINING_ROOM','ROOFTOP_TERRACE'
                                )),
    address_line1           VARCHAR(255) NOT NULL,
    address_line2           VARCHAR(255),
    city                    VARCHAR(100) NOT NULL,
    state                   VARCHAR(100) NOT NULL,
    pincode                 VARCHAR(6)   NOT NULL,
    lat                     DECIMAL(9,6) NOT NULL,
    lng                     DECIMAL(9,6) NOT NULL,
    max_guests              INTEGER      NOT NULL CHECK (max_guests >= 1),
    bedrooms                INTEGER      CHECK (bedrooms >= 0),
    bathrooms               INTEGER      CHECK (bathrooms >= 0),
    amenities               TEXT[],
    base_price_paise        BIGINT       NOT NULL CHECK (base_price_paise >= 100),
    pricing_unit            VARCHAR(10)  NOT NULL DEFAULT 'NIGHT'
                                CHECK (pricing_unit IN ('NIGHT','HOUR')),
    min_booking_hours       INTEGER      DEFAULT 1,
    ai_pricing_enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    ai_pricing_min_paise    BIGINT,
    ai_pricing_max_paise    BIGINT,
    instant_book            BOOLEAN      NOT NULL DEFAULT FALSE,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'DRAFT'
                                CHECK (status IN (
                                    'DRAFT','PENDING_VERIFICATION',
                                    'VERIFIED','PAUSED','REJECTED'
                                )),
    verification_notes      TEXT,
    august_lock_id          VARCHAR(100),
    gst_applicable          BOOLEAN      NOT NULL DEFAULT TRUE,
    gstin                   VARCHAR(15),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listings_host_id ON listings.listings(host_id);
CREATE INDEX idx_listings_status  ON listings.listings(status);
CREATE INDEX idx_listings_type    ON listings.listings(type);
CREATE INDEX idx_listings_city    ON listings.listings(city);
CREATE INDEX idx_listings_pincode ON listings.listings(pincode);

CREATE TABLE listings.listing_media (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id          UUID        NOT NULL
                            REFERENCES listings.listings(id) ON DELETE CASCADE,
    type                VARCHAR(10) NOT NULL CHECK (type IN ('PHOTO','VIDEO','3D')),
    s3_key              TEXT        NOT NULL,
    cdn_url             TEXT,
    is_primary          BOOLEAN     NOT NULL DEFAULT FALSE,
    sort_order          INTEGER     NOT NULL DEFAULT 0,
    moderation_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (moderation_status IN ('PENDING','APPROVED','REJECTED')),
    moderation_reason   TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listing_media_listing ON listings.listing_media(listing_id);
CREATE INDEX idx_listing_media_status  ON listings.listing_media(moderation_status);

CREATE TABLE listings.availability (
    id                      UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id              UUID    NOT NULL
                                REFERENCES listings.listings(id) ON DELETE CASCADE,
    date                    DATE    NOT NULL,
    is_available            BOOLEAN NOT NULL DEFAULT TRUE,
    price_override_paise    BIGINT,
    min_stay_nights         INTEGER NOT NULL DEFAULT 1,
    UNIQUE(listing_id, date)
);

CREATE INDEX idx_availability_listing_date ON listings.availability(listing_id, date);
