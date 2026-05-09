-- Prevent two ACTIVE tenancies on the same bed at the same time. NOTICE_PERIOD
-- is allowed to overlap with an incoming ACTIVE tenancy (handover window),
-- so the predicate filters to status='ACTIVE' only. Existing rows must already
-- satisfy this constraint or the migration will fail — manually reconcile any
-- duplicates first.
CREATE UNIQUE INDEX IF NOT EXISTS uq_pg_tenancies_active_bed
    ON bookings.pg_tenancies (room_type_id, bed_number)
    WHERE status = 'ACTIVE' AND bed_number IS NOT NULL;
