-- Utility meter readings for electricity and water billing
CREATE TABLE IF NOT EXISTS bookings.utility_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenancy_id UUID NOT NULL,
    utility_type VARCHAR(20) NOT NULL,
    reading_date DATE NOT NULL,
    meter_number VARCHAR(50),
    previous_reading DECIMAL(10,2) NOT NULL,
    current_reading DECIMAL(10,2) NOT NULL,
    units_consumed DECIMAL(10,2) NOT NULL,
    rate_per_unit_paise BIGINT NOT NULL,
    total_charge_paise BIGINT NOT NULL,
    billing_month INT,
    billing_year INT,
    invoice_id UUID,
    photo_url VARCHAR(500),
    recorded_by VARCHAR(20) NOT NULL DEFAULT 'HOST',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ur_tenancy FOREIGN KEY (tenancy_id) REFERENCES bookings.pg_tenancies(id)
);

CREATE INDEX idx_ur_tenancy ON bookings.utility_readings(tenancy_id);
CREATE INDEX idx_ur_period ON bookings.utility_readings(tenancy_id, billing_year, billing_month);
CREATE INDEX idx_ur_unbilled ON bookings.utility_readings(tenancy_id) WHERE invoice_id IS NULL;
