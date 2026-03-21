CREATE TABLE payments.host_tax_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id         UUID NOT NULL UNIQUE,
    gstin           VARCHAR(15),
    pan             VARCHAR(10) NOT NULL,
    business_name   VARCHAR(200),
    registered_address TEXT,
    state_code      VARCHAR(2),
    composition_scheme BOOLEAN DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payments.host_expenses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id         UUID NOT NULL,
    listing_id      UUID,
    category        VARCHAR(50) NOT NULL,
    amount_paise    BIGINT NOT NULL,
    gst_paise       BIGINT DEFAULT 0,
    description     VARCHAR(255),
    receipt_url     VARCHAR(500),
    expense_date    DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payments.gst_invoices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number  VARCHAR(30) NOT NULL UNIQUE,
    host_id         UUID NOT NULL,
    booking_id      UUID,
    guest_name      VARCHAR(200),
    guest_gstin     VARCHAR(15),
    taxable_amount  BIGINT NOT NULL,
    cgst_amount     BIGINT NOT NULL DEFAULT 0,
    sgst_amount     BIGINT NOT NULL DEFAULT 0,
    igst_amount     BIGINT NOT NULL DEFAULT 0,
    total_amount    BIGINT NOT NULL,
    invoice_date    DATE NOT NULL,
    pdf_url         VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tax_profiles_host ON payments.host_tax_profiles(host_id);
CREATE INDEX idx_expenses_host ON payments.host_expenses(host_id);
CREATE INDEX idx_gst_invoices_host ON payments.gst_invoices(host_id);
