CREATE SCHEMA IF NOT EXISTS flights;

CREATE TABLE flights.flight_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    booking_ref VARCHAR(20) NOT NULL UNIQUE,
    duffel_order_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    trip_type VARCHAR(15) NOT NULL DEFAULT 'ONE_WAY',
    cabin_class VARCHAR(20) NOT NULL DEFAULT 'ECONOMY',
    departure_city VARCHAR(100),
    departure_city_code VARCHAR(5),
    arrival_city VARCHAR(100),
    arrival_city_code VARCHAR(5),
    departure_date DATE NOT NULL,
    return_date DATE,
    airline VARCHAR(100),
    flight_number VARCHAR(20),
    is_international BOOLEAN DEFAULT false,
    passengers_json TEXT,
    slices_json TEXT,
    total_amount_paise BIGINT NOT NULL,
    tax_paise BIGINT DEFAULT 0,
    platform_fee_paise BIGINT DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'INR',
    razorpay_order_id VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    payment_status VARCHAR(20) DEFAULT 'UNPAID',
    contact_email VARCHAR(200),
    contact_phone VARCHAR(20),
    cancellation_reason VARCHAR(500),
    cancelled_at TIMESTAMPTZ,
    refund_amount_paise BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flight_bookings_user_id ON flights.flight_bookings(user_id);
CREATE INDEX idx_flight_bookings_status ON flights.flight_bookings(status);
CREATE INDEX idx_flight_bookings_departure_date ON flights.flight_bookings(departure_date);
