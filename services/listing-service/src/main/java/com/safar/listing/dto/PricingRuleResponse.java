package com.safar.listing.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PricingRuleResponse(
        UUID id,
        UUID listingId,
        UUID roomTypeId,
        String name,
        String ruleType,
        LocalDate fromDate,
        LocalDate toDate,
        String daysOfWeek,
        String priceAdjustmentType,
        Long adjustmentValue,
        Integer priority,
        Boolean isActive,
        OffsetDateTime createdAt
) {}
