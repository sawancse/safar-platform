-- V6: Make all existing guest reviews visible (switch from double-blind to Airbnb model)
-- Guest reviews are now visible immediately on creation.
-- Host reviews remain hidden until both submit or 14-day deadline.

UPDATE reviews.reviews
SET guest_review_visible = TRUE
WHERE rating IS NOT NULL
  AND guest_review_visible = FALSE;
