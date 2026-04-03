-- V55: Price history for locality trends and analytics
CREATE TABLE sale_price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_property_id UUID NOT NULL REFERENCES sale_properties(id),
    price_paise BIGINT NOT NULL,
    price_per_sqft_paise BIGINT,
    changed_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_price_history_property ON sale_price_history(sale_property_id);
CREATE INDEX idx_price_history_date ON sale_price_history(changed_at);

-- Locality-level aggregated price trends (updated by scheduled job)
CREATE TABLE locality_price_trends (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city VARCHAR(100) NOT NULL,
    locality VARCHAR(100) NOT NULL,
    month DATE NOT NULL,
    avg_price_per_sqft_paise BIGINT,
    median_price_per_sqft_paise BIGINT,
    total_listings INTEGER DEFAULT 0,
    total_sold INTEGER DEFAULT 0,
    property_type VARCHAR(30),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(city, locality, month, property_type)
);

CREATE INDEX idx_locality_trends_city ON locality_price_trends(city, locality);
CREATE INDEX idx_locality_trends_month ON locality_price_trends(month);
