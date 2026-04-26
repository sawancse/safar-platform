-- Travel insurance policies issued via Acko / ICICI Lombard / Reliance / HDFC Ergo.
-- Adapter pattern matches flight-service.

CREATE SCHEMA IF NOT EXISTS insurance;

CREATE TABLE IF NOT EXISTS insurance.insurance_policies (
    id                      UUID         PRIMARY KEY,
    user_id                 UUID         NOT NULL,
    policy_ref              VARCHAR(30)  NOT NULL UNIQUE,    -- Safar-side reference, e.g. "INS-XXXXXXXX"

    -- Provider routing
    provider                VARCHAR(20)  NOT NULL,           -- ACKO / ICICI_LOMBARD / RELIANCE_GENERAL / HDFC_ERGO
    external_policy_id      VARCHAR(100),                    -- Provider's policy id / cert number

    status                  VARCHAR(20)  NOT NULL,           -- DRAFT / PENDING_PAYMENT / ISSUED / CANCELLED / REFUNDED / EXPIRED
    coverage_type           VARCHAR(30)  NOT NULL,           -- DOMESTIC_TRAVEL / INTERNATIONAL_TRAVEL / STUDENT_TRAVEL

    -- Trip context
    trip_origin_code        VARCHAR(5),
    trip_destination_code   VARCHAR(5),
    trip_origin_country     VARCHAR(2)   NOT NULL DEFAULT 'IN',
    trip_destination_country VARCHAR(2)  NOT NULL DEFAULT 'IN',
    trip_start_date         DATE         NOT NULL,
    trip_end_date           DATE         NOT NULL,

    -- Policy holder + insured travellers
    insured_count           INT          NOT NULL DEFAULT 1,
    insured_json            TEXT,                            -- JSON array: [{firstName, lastName, dob, gender, passport}]
    contact_email           VARCHAR(200),
    contact_phone           VARCHAR(20),

    -- Pricing
    premium_paise           BIGINT       NOT NULL,           -- amount user pays
    sum_insured_paise       BIGINT,                          -- max coverage
    currency                VARCHAR(3)   NOT NULL DEFAULT 'INR',

    -- Payment integration (Razorpay)
    razorpay_order_id       VARCHAR(100),
    razorpay_payment_id     VARCHAR(100),
    payment_status          VARCHAR(20)  NOT NULL DEFAULT 'UNPAID',

    -- Lifecycle metadata
    issued_at               TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     VARCHAR(500),
    refund_amount_paise     BIGINT,

    -- Document urls (cert PDF, brochure, etc.)
    certificate_url         VARCHAR(500),

    -- Multi-country ready
    -- (provider may differ per country in future expansion)

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_insurance_user_id     ON insurance.insurance_policies (user_id);
CREATE INDEX IF NOT EXISTS idx_insurance_status      ON insurance.insurance_policies (status);
CREATE INDEX IF NOT EXISTS idx_insurance_provider    ON insurance.insurance_policies (provider);
CREATE INDEX IF NOT EXISTS idx_insurance_external    ON insurance.insurance_policies (provider, external_policy_id);
CREATE INDEX IF NOT EXISTS idx_insurance_trip_dates  ON insurance.insurance_policies (trip_start_date, trip_end_date);
