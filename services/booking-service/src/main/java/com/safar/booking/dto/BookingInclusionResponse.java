package com.safar.booking.dto;

import java.util.UUID;

public record BookingInclusionResponse(
        UUID id,
        UUID inclusionId,
        String category,
        String name,
        String description,
        String inclusionMode,
        Long chargePaise,
        String chargeType,
        Integer discountPercent,
        String terms,
        Integer quantity,
        Long totalPaise
) {}
