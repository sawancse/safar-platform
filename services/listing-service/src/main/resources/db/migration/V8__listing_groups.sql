CREATE TABLE listings.listing_groups (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id             UUID NOT NULL,
    name                VARCHAR(100) NOT NULL,
    bundle_discount_pct INT DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE listings.listing_group_members (
    group_id    UUID NOT NULL,
    listing_id  UUID NOT NULL,
    PRIMARY KEY (group_id, listing_id)
);
