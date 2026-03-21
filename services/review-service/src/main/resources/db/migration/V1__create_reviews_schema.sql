CREATE SCHEMA IF NOT EXISTS reviews;

CREATE TABLE reviews.reviews (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID        NOT NULL UNIQUE,
    listing_id      UUID        NOT NULL,
    guest_id        UUID        NOT NULL,
    host_id         UUID        NOT NULL,
    rating          SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    reply           TEXT,
    replied_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reviews_listing ON reviews.reviews(listing_id);
CREATE INDEX idx_reviews_guest   ON reviews.reviews(guest_id);
CREATE INDEX idx_reviews_host    ON reviews.reviews(host_id);
