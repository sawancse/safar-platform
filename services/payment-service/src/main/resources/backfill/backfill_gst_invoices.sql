-- One-time backfill: create host GST invoices for already-COMPLETED bookings.
--
-- Earnings/Invoices/TDS read from payments.gst_invoices, which was never populated
-- because invoice generation on booking completion didn't exist until this change.
-- Going forward the booking.invoice.requested Kafka flow covers new completions;
-- this script covers bookings completed before the fix shipped.
--
-- Safe to re-run: the NOT EXISTS guard makes it idempotent (one invoice per booking).
-- 'SAF-GST-BF-' prefix keeps these distinct from runtime-generated numbers.
INSERT INTO payments.gst_invoices
    (invoice_number, host_id, booking_id, guest_name,
     taxable_amount, cgst_amount, sgst_amount, igst_amount,
     total_amount, invoice_date)
SELECT
    'SAF-GST-BF-' || to_char(COALESCE(b.completed_at, now()), 'YYYYMM') || '-'
        || lpad((ROW_NUMBER() OVER (ORDER BY b.completed_at NULLS LAST, b.id))::text, 4, '0'),
    b.host_id,
    b.id,
    NULLIF(trim(COALESCE(b.guest_first_name, '') || ' ' || COALESCE(b.guest_last_name, '')), ''),
    round(b.total_amount_paise * 100.0 / 118.0)::bigint                              AS taxable_amount,
    (b.total_amount_paise - round(b.total_amount_paise * 100.0 / 118.0)::bigint) / 2 AS cgst_amount,
    (b.total_amount_paise - round(b.total_amount_paise * 100.0 / 118.0)::bigint)
        - ((b.total_amount_paise - round(b.total_amount_paise * 100.0 / 118.0)::bigint) / 2) AS sgst_amount,
    0                                                                               AS igst_amount,
    b.total_amount_paise,
    COALESCE(b.completed_at::date, current_date)
FROM bookings.bookings b
WHERE b.status = 'COMPLETED'
  AND b.host_id IS NOT NULL
  AND b.total_amount_paise > 0
  AND NOT EXISTS (
        SELECT 1 FROM payments.gst_invoices g WHERE g.booking_id = b.id
  );
