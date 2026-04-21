-- Extra non-cooking services a customer can request for an event
-- (photography, DJ, decor, MC, makeup, puja, etc.). Stored as a JSON
-- array of { key, label, notes, priceEstimatePaise } so the event
-- flow stays flexible — we don't have to add a column per service.

ALTER TABLE chefs.event_bookings
    ADD COLUMN IF NOT EXISTS services_json TEXT;
