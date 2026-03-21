package com.safar.booking.dto;

import java.util.List;
import java.util.UUID;

public record GroupBookingResult(
        UUID groupBookingId,
        List<UUID> bookingIds,
        Long totalAmountPaise,
        Boolean discountApplied
) {}
