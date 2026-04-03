package com.safar.listing.dto;

import com.safar.listing.entity.enums.FinancingType;
import com.safar.listing.entity.enums.PossessionTimeline;

import java.util.List;

public record BuyerProfileRequest(
        List<String> preferredCities,
        List<String> preferredLocalities,
        Long budgetMinPaise,
        Long budgetMaxPaise,
        List<String> preferredBhk,
        List<String> preferredTypes,
        FinancingType financingType,
        PossessionTimeline possessionTimeline,
        Boolean alertsEnabled
) {}
