-- Adds a canonical FK from pg_tenancies → bookings (source_booking_id).
--
-- Before V51, the relationship was inferred client-side by matching
-- guestId === tenantId on the same listing — fragile when a guest has
-- more than one booking on the same property. This column makes it explicit
-- and is set at creation time by BookingService.checkInBooking (auto-creation)
-- and PgTenancyService.createTenancyFromRequest (when caller knows the booking).

ALTER TABLE bookings.pg_tenancies
    ADD COLUMN IF NOT EXISTS source_booking_id UUID;

-- Backfill: for each existing tenancy, pick the most-recent matching booking
-- on the same listing for the same guest. Prefer CHECKED_IN/COMPLETED
-- (those produced a tenancy in the first place), fall back to CONFIRMED.
WITH ranked AS (
    SELECT
        t.id  AS tenancy_id,
        b.id  AS booking_id,
        ROW_NUMBER() OVER (
            PARTITION BY t.id
            ORDER BY
                CASE b.status
                    WHEN 'CHECKED_IN' THEN 0
                    WHEN 'COMPLETED'  THEN 1
                    WHEN 'CONFIRMED'  THEN 2
                    ELSE 3
                END,
                ABS(EXTRACT(EPOCH FROM (b.check_in - t.move_in_date::timestamp))),
                b.created_at DESC
        ) AS rn
    FROM bookings.pg_tenancies t
    JOIN bookings.bookings b
      ON b.guest_id   = t.tenant_id
     AND b.listing_id = t.listing_id
    WHERE t.source_booking_id IS NULL
)
UPDATE bookings.pg_tenancies t
   SET source_booking_id = r.booking_id
  FROM ranked r
 WHERE r.tenancy_id = t.id
   AND r.rn = 1;

CREATE INDEX IF NOT EXISTS idx_pg_tenancies_source_booking
    ON bookings.pg_tenancies (source_booking_id);
