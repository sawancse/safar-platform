-- ============================================================
-- Inquiry Token/Booking Amount Payment + Chat Link
-- ============================================================

ALTER TABLE listings.property_inquiries ADD COLUMN token_amount_paise BIGINT DEFAULT 0;
ALTER TABLE listings.property_inquiries ADD COLUMN payment_status VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE listings.property_inquiries ADD COLUMN razorpay_payment_id VARCHAR(100);
ALTER TABLE listings.property_inquiries ADD COLUMN razorpay_order_id VARCHAR(100);
ALTER TABLE listings.property_inquiries ADD COLUMN razorpay_refund_id VARCHAR(100);
ALTER TABLE listings.property_inquiries ADD COLUMN paid_at TIMESTAMPTZ;
ALTER TABLE listings.property_inquiries ADD COLUMN refunded_at TIMESTAMPTZ;
ALTER TABLE listings.property_inquiries ADD COLUMN unit_type_id UUID;
ALTER TABLE listings.property_inquiries ADD COLUMN preferred_floor VARCHAR(20);
ALTER TABLE listings.property_inquiries ADD COLUMN conversation_id UUID;
