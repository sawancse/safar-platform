package com.safar.listing.dto;

import com.safar.listing.entity.enums.FurnishingStatus;

import java.util.List;
import java.util.UUID;

public record UnitTypeResponse(
        UUID id,
        UUID projectId,
        String name,
        Integer bhk,
        Integer carpetAreaSqft,
        Integer builtUpAreaSqft,
        Integer superBuiltUpAreaSqft,
        Long basePricePaise,
        Long floorRisePaise,
        Long facingPremiumPaise,
        Integer premiumFloorsFrom,
        Integer totalUnits,
        Integer availableUnits,
        Integer bathrooms,
        Integer balconies,
        FurnishingStatus furnishing,
        String floorPlanUrl,
        String unitLayoutUrl,
        List<String> photos,
        // Computed
        Long pricePerSqftPaise
) {}
