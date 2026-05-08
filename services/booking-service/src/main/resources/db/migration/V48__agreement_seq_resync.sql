-- Resync bookings.agreement_ref_seq with the highest numeric suffix already
-- written into tenancy_agreements.agreement_number.
--
-- Background: agreement numbers were previously generated from a static
-- in-memory counter that reset to 1000 on every JVM restart, causing
-- duplicate-key collisions on agreement_number (e.g. AGR-2026-1001).
-- The service now reads from this Postgres sequence; bump it past whatever
-- is already in the table so the next nextval() doesn't collide.
SELECT setval(
    'bookings.agreement_ref_seq',
    GREATEST(
        1000,
        COALESCE(
            (SELECT MAX(CAST(split_part(agreement_number, '-', 3) AS BIGINT))
               FROM bookings.tenancy_agreements
              WHERE agreement_number ~ '^AGR-[0-9]+-[0-9]+$'),
            1000
        )
    ),
    true  -- 'true' => next nextval() returns value+1, so we skip past the max
);
