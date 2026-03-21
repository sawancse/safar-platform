ALTER TABLE bookings.bookings
    ADD COLUMN guest_first_name   VARCHAR(100),
    ADD COLUMN guest_last_name    VARCHAR(100),
    ADD COLUMN guest_email        VARCHAR(255),
    ADD COLUMN guest_phone        VARCHAR(20),
    ADD COLUMN booking_for        VARCHAR(10) DEFAULT 'self',
    ADD COLUMN travel_for_work    BOOLEAN DEFAULT false,
    ADD COLUMN airport_shuttle    BOOLEAN DEFAULT false,
    ADD COLUMN special_requests   TEXT;
