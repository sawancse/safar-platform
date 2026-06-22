package com.safar.booking.dto;

/** Result of validating a coupon against a cart. {@code discountPaise} is what would
 *  be deducted; {@code valid=false} carries a human {@code message} for the UI. */
public record CouponValidationResult(
        boolean valid,
        String code,
        String message,
        long discountPaise,
        String discountType
) {
    public static CouponValidationResult invalid(String code, String message) {
        return new CouponValidationResult(false, code, message, 0L, null);
    }
}
