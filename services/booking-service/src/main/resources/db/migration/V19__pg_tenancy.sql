-- PG Tenancy Model: Subscription-like entity with recurring billing
CREATE TABLE IF NOT EXISTS bookings.pg_tenancies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenancy_ref VARCHAR(20) NOT NULL UNIQUE,
    tenant_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    room_type_id UUID,
    bed_number VARCHAR(10),
    sharing_type VARCHAR(30) NOT NULL DEFAULT 'PRIVATE',
    move_in_date DATE NOT NULL,
    move_out_date DATE,
    notice_period_days INT NOT NULL DEFAULT 30,
    monthly_rent_paise BIGINT NOT NULL,
    security_deposit_paise BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    meals_included BOOLEAN NOT NULL DEFAULT FALSE,
    laundry_included BOOLEAN NOT NULL DEFAULT FALSE,
    wifi_included BOOLEAN NOT NULL DEFAULT FALSE,
    total_monthly_paise BIGINT NOT NULL,
    billing_day INT NOT NULL DEFAULT 1,
    next_billing_date DATE,
    razorpay_subscription_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS bookings.tenancy_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenancy_id UUID NOT NULL,
    invoice_number VARCHAR(30) NOT NULL UNIQUE,
    billing_month INT NOT NULL,
    billing_year INT NOT NULL,
    rent_paise BIGINT NOT NULL,
    packages_paise BIGINT NOT NULL DEFAULT 0,
    electricity_paise BIGINT NOT NULL DEFAULT 0,
    total_paise BIGINT NOT NULL,
    gst_paise BIGINT NOT NULL DEFAULT 0,
    grand_total_paise BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    due_date DATE NOT NULL,
    paid_date DATE,
    razorpay_payment_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ti_tenancy FOREIGN KEY (tenancy_id) REFERENCES bookings.pg_tenancies(id)
);

CREATE INDEX idx_pgt_tenant ON bookings.pg_tenancies(tenant_id);
CREATE INDEX idx_pgt_listing ON bookings.pg_tenancies(listing_id);
CREATE INDEX idx_pgt_status ON bookings.pg_tenancies(status);
CREATE INDEX idx_pgt_billing ON bookings.pg_tenancies(next_billing_date);
CREATE INDEX idx_ti_tenancy ON bookings.tenancy_invoices(tenancy_id);
CREATE INDEX idx_ti_status ON bookings.tenancy_invoices(status);

CREATE SEQUENCE IF NOT EXISTS bookings.tenancy_ref_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS bookings.invoice_seq START WITH 1000 INCREMENT BY 1;
