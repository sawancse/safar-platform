-- V35: Add PG, Co-living, Hotel listing types
-- Update CHECK constraint to include new types
ALTER TABLE listings.listings DROP CONSTRAINT IF EXISTS listings_type_check;
ALTER TABLE listings.listings ADD CONSTRAINT listings_type_check
  CHECK (type IN ('HOME','ROOM','UNIQUE','COMMERCIAL','VILLA','RESORT','HOMESTAY',
    'HOSTEL','GUESTHOUSE','FARMSTAY','BNB','LODGE','CHALET','APARTMENT','VACATION_HOME',
    'PG','COLIVING','HOTEL','BUDGET_HOTEL','HOSTEL_DORM'));

-- Update pricing_unit CHECK to include MONTH
ALTER TABLE listings.listings DROP CONSTRAINT IF EXISTS listings_pricing_unit_check;
ALTER TABLE listings.listings ADD CONSTRAINT listings_pricing_unit_check
  CHECK (pricing_unit IN ('NIGHT','HOUR','MONTH'));
