package com.safar.listing.dto;

import com.safar.listing.entity.enums.FurnishingStatus;
import com.safar.listing.entity.enums.UnitKind;

import java.util.List;
import java.util.UUID;

public record UnitTypeResponse(
        UUID id,
        UUID projectId,
        String name,
        UnitKind unitKind,
        Integer bhk,
        Integer carpetAreaSqft,
        Integer builtUpAreaSqft,
        Integer superBuiltUpAreaSqft,
        Long basePricePaise,
        Long pricePerSqftPaise,
        Long floorRisePaise,
        Long facingPremiumPaise,
        Integer premiumFloorsFrom,
        Integer totalUnits,
        Integer availableUnits,
        Integer bathrooms,
        Integer balconies,
        FurnishingStatus furnishing,
        // Plot fields (PLOT only — null for UNIT)
        Integer plotAreaSqft,
        Integer plotLengthFt,
        Integer plotBreadthFt,
        Boolean cornerPlot,
        String facing,
        // Media
        String floorPlanUrl,
        String unitLayoutUrl,
        List<String> photos
) {}
