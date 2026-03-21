CREATE TABLE listings.vpn_listings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID NOT NULL,
    host_id         UUID NOT NULL,
    commission_pct  INT NOT NULL DEFAULT 10,
    open_to_network BOOLEAN NOT NULL DEFAULT false,
    min_stay_nights INT DEFAULT 1,
    available_from  DATE,
    available_to    DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE listings.vpn_referrals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_id     UUID NOT NULL,
    listing_id      UUID NOT NULL,
    booking_id      UUID,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    commission_paise BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at         TIMESTAMPTZ
);
CREATE INDEX idx_vpn_listings_network ON listings.vpn_listings(open_to_network, available_from, available_to);
