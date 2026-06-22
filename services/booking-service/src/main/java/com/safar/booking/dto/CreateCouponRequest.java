package com.safar.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Create/update a coupon. scope + listingId are ignored for host-created coupons
 *  (forced to HOST + the caller's ownership). */
public record CreateCouponRequest(
        String code,
        String scope,               // PLATFORM | HOST (admin only; host forced to HOST)
        UUID listingId,             // optional restriction to one listing
        String discountType,        // PERCENT | FLAT
        Integer percentOff,
        Long maxDiscountPaise,
        Long flatOffPaise,
        Long minBookingPaise,
        LocalDate validFrom,
        LocalDate validUntil,
        Integer usageLimit,
        Integer perUserLimit,
        Boolean active,
        String description
) {}
