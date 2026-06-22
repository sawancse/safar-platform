package com.safar.booking.dto;

import java.util.UUID;

public record ValidateCouponRequest(
        String code,
        UUID listingId,
        long subtotalPaise
) {}
