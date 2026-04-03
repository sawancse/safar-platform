package com.safar.listing.dto;

public record UnitPriceCalculation(
        String unitTypeName,
        Integer bhk,
        Long basePricePaise,
        Integer floor,
        Long floorRisePaise,
        Boolean preferredFacing,
        Long facingPremiumPaise,
        Long totalPricePaise,
        Long pricePerSqftPaise,
        Long estimatedEmiPaise // at 8.5% for 20 years
) {}
