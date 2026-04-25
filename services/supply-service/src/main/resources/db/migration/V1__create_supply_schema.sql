-- V1: Supply Chain Management — Phase 1 schema
-- Owns: suppliers we buy FROM (distinct from chefs.partner_vendors who sell TO our customers),
-- their catalogs, purchase orders + line items, current stock, and the append-only
-- stock movement ledger that's the source of truth for on_hand_qty.
--
-- All amounts in paise. All prices/quantities are NUMERIC(12,2) for partial units (0.5 kg).

CREATE SCHEMA IF NOT EXISTS supply;

-- ── Suppliers ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS supply.suppliers (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_name         VARCHAR(160) NOT NULL,
    owner_name            VARCHAR(120),
    phone                 VARCHAR(20)  NOT NULL,
    email                 VARCHAR(160),
    whatsapp              VARCHAR(20),
    gst                   VARCHAR(20),
    pan                   VARCHAR(15),
    bank_account          VARCHAR(40),
    bank_ifsc             VARCHAR(15),
    bank_holder           VARCHAR(120),
    address               TEXT,
    categories            TEXT[]       DEFAULT '{}',  -- GROCERY / BAKERY / DECOR / PG_LINEN / MAINTENANCE
    service_cities        TEXT[]       DEFAULT '{}',  -- empty = delivers anywhere
    lead_time_days        INT          DEFAULT 2,
    payment_terms         VARCHAR(40)  DEFAULT 'NET_15',  -- NET_0 / NET_7 / NET_15 / NET_30
    credit_limit_paise    BIGINT       DEFAULT 0,
    kyc_status            VARCHAR(20)  DEFAULT 'PENDING',  -- PENDING / VERIFIED / REJECTED
    kyc_notes             TEXT,
    rating_avg            NUMERIC(3,2),
    rating_count          INT          DEFAULT 0,
    pos_completed         INT          DEFAULT 0,
    active                BOOLEAN      DEFAULT TRUE,
    notes                 TEXT,
    created_at            TIMESTAMPTZ  DEFAULT now(),
    updated_at            TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_suppliers_active     ON supply.suppliers (active);
CREATE INDEX IF NOT EXISTS idx_suppliers_kyc        ON supply.suppliers (kyc_status);
CREATE INDEX IF NOT EXISTS idx_suppliers_categories ON supply.suppliers USING GIN (categories);
CREATE INDEX IF NOT EXISTS idx_suppliers_cities     ON supply.suppliers USING GIN (service_cities);

-- ── Supplier catalog ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS supply.supplier_catalog_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id     UUID         NOT NULL REFERENCES supply.suppliers(id) ON DELETE CASCADE,
    item_key        VARCHAR(60)  NOT NULL,   -- canonical key, e.g. flour_maida
    item_label      VARCHAR(120) NOT NULL,
    category        VARCHAR(30)  NOT NULL,
    unit            VARCHAR(20)  NOT NULL,   -- KG/GRAM/LITRE/PIECE/METRE/DOZEN
    price_paise     BIGINT       NOT NULL,
    moq_qty         NUMERIC(12,2),
    pack_size       NUMERIC(12,2),
    lead_time_days  INT,                     -- override supplier default
    active          BOOLEAN      DEFAULT TRUE,
    notes           TEXT,
    created_at      TIMESTAMPTZ  DEFAULT now(),
    updated_at      TIMESTAMPTZ  DEFAULT now(),
    CONSTRAINT uk_catalog_supplier_item UNIQUE (supplier_id, item_key)
);

CREATE INDEX IF NOT EXISTS idx_catalog_supplier ON supply.supplier_catalog_items (supplier_id, active);
CREATE INDEX IF NOT EXISTS idx_catalog_item_key ON supply.supplier_catalog_items (item_key, active);
CREATE INDEX IF NOT EXISTS idx_catalog_category ON supply.supplier_catalog_items (category, active);

-- ── Purchase Orders ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS supply.purchase_orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_number           VARCHAR(20)  UNIQUE NOT NULL,   -- PO-YYYYMM-NNNN
    supplier_id         UUID         NOT NULL REFERENCES supply.suppliers(id),
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    -- DRAFT / ISSUED / ACKNOWLEDGED / IN_TRANSIT / DELIVERED / INVOICED / PAID / CANCELLED
    ordered_at          TIMESTAMPTZ,
    expected_delivery   DATE,
    delivered_at        TIMESTAMPTZ,
    invoice_number      VARCHAR(60),
    invoice_paise       BIGINT,
    invoiced_at         TIMESTAMPTZ,
    paid_at             TIMESTAMPTZ,
    payment_ref         VARCHAR(60),
    cancelled_at        TIMESTAMPTZ,
    cancel_reason       TEXT,
    total_paise         BIGINT       NOT NULL DEFAULT 0,
    tax_paise           BIGINT       DEFAULT 0,
    grand_total_paise   BIGINT       DEFAULT 0,
    delivery_address    TEXT,
    created_by_user_id  UUID,
    admin_notes         TEXT,
    created_at          TIMESTAMPTZ  DEFAULT now(),
    updated_at          TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_po_status              ON supply.purchase_orders (status);
CREATE INDEX IF NOT EXISTS idx_po_supplier_status     ON supply.purchase_orders (supplier_id, status);
CREATE INDEX IF NOT EXISTS idx_po_expected_delivery   ON supply.purchase_orders (expected_delivery)
  WHERE status IN ('ISSUED','ACKNOWLEDGED','IN_TRANSIT');

-- ── Purchase Order Line Items ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS supply.purchase_order_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id             UUID         NOT NULL REFERENCES supply.purchase_orders(id) ON DELETE CASCADE,
    catalog_item_id   UUID         REFERENCES supply.supplier_catalog_items(id) ON DELETE SET NULL,
    item_key          VARCHAR(60)  NOT NULL,
    item_label        VARCHAR(120) NOT NULL,
    category          VARCHAR(30)  NOT NULL,
    unit              VARCHAR(20)  NOT NULL,
    qty               NUMERIC(12,2) NOT NULL,
    unit_price_paise  BIGINT       NOT NULL,
    line_total_paise  BIGINT       NOT NULL,
    received_qty      NUMERIC(12,2) DEFAULT 0,
    notes             TEXT,
    CONSTRAINT uk_po_item UNIQUE (po_id, item_key)
);

CREATE INDEX IF NOT EXISTS idx_po_items_po ON supply.purchase_order_items (po_id);

-- ── Stock Items (current on-hand snapshot) ─────────────────────────
CREATE TABLE IF NOT EXISTS supply.stock_items (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_key               VARCHAR(60)  UNIQUE NOT NULL,
    item_label             VARCHAR(120) NOT NULL,
    category               VARCHAR(30)  NOT NULL,
    unit                   VARCHAR(20)  NOT NULL,
    on_hand_qty            NUMERIC(12,2) DEFAULT 0,
    reserved_qty           NUMERIC(12,2) DEFAULT 0,
    reorder_point          NUMERIC(12,2),
    reorder_qty            NUMERIC(12,2),
    last_unit_cost_paise   BIGINT,
    last_received_at       TIMESTAMPTZ,
    active                 BOOLEAN      DEFAULT TRUE,
    created_at             TIMESTAMPTZ  DEFAULT now(),
    updated_at             TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_stock_category ON supply.stock_items (category, active);
CREATE INDEX IF NOT EXISTS idx_stock_low      ON supply.stock_items (on_hand_qty);

-- ── Stock Movements (append-only ledger) ───────────────────────────
CREATE TABLE IF NOT EXISTS supply.stock_movements (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_item_id          UUID         NOT NULL REFERENCES supply.stock_items(id) ON DELETE CASCADE,
    item_key               VARCHAR(60)  NOT NULL,
    direction              VARCHAR(10)  NOT NULL,   -- IN / OUT / ADJUSTMENT
    qty                    NUMERIC(12,2) NOT NULL,
    reason                 VARCHAR(40)  NOT NULL,   -- PO_RECEIVED / EVENT_CONSUMED / ADJUSTMENT_DAMAGE / ADJUSTMENT_COUNT / RETURN
    ref_type               VARCHAR(30),             -- PO / EVENT_BOOKING / MANUAL
    ref_id                 UUID,
    unit_cost_paise        BIGINT,
    performed_by_user_id   UUID,
    notes                  TEXT,
    created_at             TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_movements_stock_recent ON supply.stock_movements (stock_item_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_movements_ref          ON supply.stock_movements (ref_type, ref_id);
