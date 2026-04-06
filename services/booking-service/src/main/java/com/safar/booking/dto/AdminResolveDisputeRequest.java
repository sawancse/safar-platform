package com.safar.booking.dto;

import java.util.UUID;

public record AdminResolveDisputeRequest(
        UUID deductionId,
        String decision,
        Long adjustedPaise,
        String notes
) {}
