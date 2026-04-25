-- V2: Supplier integration metadata.
-- Adds the columns needed to dispatch POs to external supplier APIs
-- (Udaan, FernsNPetals B2B, Amazon Business, ...). Strictly additive —
-- existing MANUAL flow is unchanged because every existing row defaults
-- to integration_type='MANUAL'.

ALTER TABLE supply.suppliers
  ADD COLUMN IF NOT EXISTS integration_type   VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
  ADD COLUMN IF NOT EXISTS integration_config JSONB,
  ADD COLUMN IF NOT EXISTS catalog_synced_at  TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_suppliers_integration_type
  ON supply.suppliers (integration_type);

ALTER TABLE supply.purchase_orders
  ADD COLUMN IF NOT EXISTS external_ref       VARCHAR(60),
  ADD COLUMN IF NOT EXISTS external_status    VARCHAR(40),
  ADD COLUMN IF NOT EXISTS external_synced_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS external_error     TEXT;

CREATE INDEX IF NOT EXISTS idx_po_external_ref
  ON supply.purchase_orders (external_ref);
