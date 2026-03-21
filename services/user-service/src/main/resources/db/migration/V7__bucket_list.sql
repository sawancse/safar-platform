CREATE TABLE users.guest_bucket_list (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id    UUID NOT NULL,
    listing_id  UUID NOT NULL,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes       TEXT,
    UNIQUE(guest_id, listing_id)
);

CREATE TABLE users.discovery_feed_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id    UUID NOT NULL,
    listing_ids TEXT,
    algorithm   VARCHAR(50),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
