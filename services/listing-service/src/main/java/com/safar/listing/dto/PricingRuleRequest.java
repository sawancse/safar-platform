package com.safar.listing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record PricingRuleRequest(
        UUID roomTypeId,
        @NotBlank String name,
        @NotBlank String ruleType,
        LocalDate fromDate,
        LocalDate toDate,
        String daysOfWeek,
        @NotBlank String priceAdjustmentType,
        @NotNull Long adjustmentValue,
        Integer priority,
        Boolean isActive
) {}
