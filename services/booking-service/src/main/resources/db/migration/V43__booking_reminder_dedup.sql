-- Track when payment reminders were sent so the 5-minute scheduler
-- doesn't fire the same reminder repeatedly for a single booking.
ALTER TABLE bookings.bookings
    ADD COLUMN IF NOT EXISTS reminder_sent_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS reminder_urgent_sent_at TIMESTAMP WITH TIME ZONE;
