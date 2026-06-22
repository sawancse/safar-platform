package com.safar.booking.dto;

import com.safar.booking.entity.Coupon;

import java.time.LocalDate;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        String scope,
        UUID ownerId,
        UUID listingId,
        String discountType,
        Integer percentOff,
        Long maxDiscountPaise,
        Long flatOffPaise,
        Long minBookingPaise,
        LocalDate validFrom,
        LocalDate validUntil,
        Integer usageLimit,
        Integer usedCount,
        Integer perUserLimit,
        Boolean active,
        String description
) {
    public static CouponResponse from(Coupon c) {
        return new CouponResponse(
                c.getId(), c.getCode(), c.getScope(), c.getOwnerId(), c.getListingId(),
                c.getDiscountType(), c.getPercentOff(), c.getMaxDiscountPaise(), c.getFlatOffPaise(),
                c.getMinBookingPaise(), c.getValidFrom(), c.getValidUntil(),
                c.getUsageLimit(), c.getUsedCount(), c.getPerUserLimit(), c.getActive(), c.getDescription());
    }
}
