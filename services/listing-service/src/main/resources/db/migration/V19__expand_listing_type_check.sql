-- V19: Expand listing type check constraint to allow new property types
ALTER TABLE listings.listings DROP CONSTRAINT IF EXISTS listings_type_check;
ALTER TABLE listings.listings ADD CONSTRAINT listings_type_check
    CHECK (type IN ('HOME', 'ROOM', 'UNIQUE', 'COMMERCIAL',
                    'VILLA', 'RESORT', 'HOMESTAY', 'HOSTEL', 'GUESTHOUSE',
                    'FARMSTAY', 'BNB', 'LODGE', 'CHALET', 'APARTMENT', 'VACATION_HOME'));
