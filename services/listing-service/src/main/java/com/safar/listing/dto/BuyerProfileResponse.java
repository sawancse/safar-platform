package com.safar.listing.dto;

import com.safar.listing.entity.enums.FinancingType;
import com.safar.listing.entity.enums.PossessionTimeline;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BuyerProfileResponse(
        UUID id,
        UUID userId,
        List<String> preferredCities,
        List<String> preferredLocalities,
        Long budgetMinPaise,
        Long budgetMaxPaise,
        List<String> preferredBhk,
        List<String> preferredTypes,
        FinancingType financingType,
        PossessionTimeline possessionTimeline,
        Boolean alertsEnabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
