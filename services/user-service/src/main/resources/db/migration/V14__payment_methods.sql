CREATE TABLE users.payment_methods (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    type            VARCHAR(20) NOT NULL,
    label           VARCHAR(100),
    is_default      BOOLEAN DEFAULT FALSE,

    -- UPI
    upi_id          VARCHAR(80),

    -- Card (we store only last4 + metadata, never full card number)
    card_last4      VARCHAR(4),
    card_network    VARCHAR(20),
    card_holder     VARCHAR(100),
    card_expiry     VARCHAR(7),

    -- Net Banking
    bank_name       VARCHAR(100),
    bank_account_last4 VARCHAR(4),

    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_payment_methods_user_id ON users.payment_methods(user_id);
