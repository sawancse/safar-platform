-- Fix existing tenancies that all have bed_number='1-A' (hardcoded bug).
-- Assign sequential bed numbers per room_type_id for ACTIVE tenancies.

WITH numbered AS (
    SELECT id, room_type_id,
           ROW_NUMBER() OVER (PARTITION BY room_type_id ORDER BY created_at) AS rn
    FROM bookings.pg_tenancies
    WHERE status = 'ACTIVE'
)
UPDATE bookings.pg_tenancies t
SET bed_number = ((n.rn - 1) / 2 + 1) || '-' ||
                 CHR((65 + ((n.rn - 1) % 2))::int)
FROM numbered n
WHERE t.id = n.id;
-- Default assumes TWO_SHARING (2 beds per room): Room 1 → 1-A, 1-B, Room 2 → 2-A, 2-B, etc.
