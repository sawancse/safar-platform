package com.safar.listing.dto;

import com.safar.listing.entity.enums.FurnishingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UnitTypeRequest(
        @NotBlank String name,
        @NotNull Integer bhk,
        Integer carpetAreaSqft,
        Integer builtUpAreaSqft,
        Integer superBuiltUpAreaSqft,
        @NotNull @Positive Long basePricePaise,
        Long floorRisePaise,
        Long facingPremiumPaise,
        Integer premiumFloorsFrom,
        Integer totalUnits,
        Integer bathrooms,
        Integer balconies,
        FurnishingStatus furnishing,
        String floorPlanUrl,
        String unitLayoutUrl,
        List<String> photos
) {}
