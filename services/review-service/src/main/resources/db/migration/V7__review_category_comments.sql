-- V7: Add category comments to reviews
-- Stores per-category text feedback as JSON: {"cleanliness":"Bathroom was spotless","location":"Near metro"}
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS category_comments TEXT;
