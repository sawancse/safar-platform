-- Per-room-type security deposit (Zolo style)
-- Listing-level deposit remains as default; room-level overrides it when set
ALTER TABLE listings.room_types ADD COLUMN security_deposit_paise BIGINT;
ALTER TABLE listings.room_types ADD COLUMN deposit_type VARCHAR(20) DEFAULT 'REFUNDABLE';
