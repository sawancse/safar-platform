CREATE TABLE listings.pricing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings.listings(id) ON DELETE CASCADE,
    room_type_id UUID,
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(20) NOT NULL,
    from_date DATE,
    to_date DATE,
    days_of_week TEXT,
    price_adjustment_type VARCHAR(20) NOT NULL,
    adjustment_value BIGINT NOT NULL,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_pricing_rules_listing ON listings.pricing_rules(listing_id);
