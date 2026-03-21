CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE payments.host_invoices (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id             UUID NOT NULL,
    razorpay_sub_id     VARCHAR(100),
    invoice_number      VARCHAR(30) UNIQUE NOT NULL,
    tier                VARCHAR(20) NOT NULL,
    amount_paise        BIGINT NOT NULL,
    gst_amount_paise    BIGINT NOT NULL,
    total_paise         BIGINT NOT NULL,
    status              VARCHAR(20) DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT','ISSUED','PAID','OVERDUE')),
    billing_period_start DATE NOT NULL,
    billing_period_end   DATE NOT NULL,
    pdf_s3_key          TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE payments.payments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id              UUID,
    razorpay_order_id       VARCHAR(100) UNIQUE NOT NULL,
    razorpay_payment_id     VARCHAR(100) UNIQUE,
    amount_paise            BIGINT NOT NULL,
    currency                VARCHAR(3) DEFAULT 'INR',
    method                  VARCHAR(20),
    status                  VARCHAR(20) DEFAULT 'CREATED'
                                CHECK (status IN ('CREATED','AUTHORIZED','CAPTURED','REFUNDED','FAILED')),
    gst_invoice_number      VARCHAR(30) UNIQUE,
    captured_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE payments.payouts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id             UUID NOT NULL,
    booking_id          UUID,
    amount_paise        BIGINT NOT NULL,
    tds_paise           BIGINT DEFAULT 0,
    net_amount_paise    BIGINT NOT NULL,
    method              VARCHAR(20) NOT NULL CHECK (method IN ('UPI','NEFT','IMPS')),
    upi_id              VARCHAR(100),
    razorpay_payout_id  VARCHAR(100),
    status              VARCHAR(20) DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED')),
    initiated_at        TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_invoices_host   ON payments.host_invoices(host_id);
CREATE INDEX idx_payments_booking ON payments.payments(booking_id);
CREATE INDEX idx_payouts_host    ON payments.payouts(host_id);
