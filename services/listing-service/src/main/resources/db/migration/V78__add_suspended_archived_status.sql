-- Add SUSPENDED and ARCHIVED to listings status check constraint
ALTER TABLE listings.listings
    DROP CONSTRAINT listings_status_check;

ALTER TABLE listings.listings
    ADD CONSTRAINT listings_status_check
        CHECK (status IN (
            'DRAFT','PENDING_VERIFICATION',
            'VERIFIED','PAUSED','REJECTED',
            'ARCHIVED','SUSPENDED'
        ));
