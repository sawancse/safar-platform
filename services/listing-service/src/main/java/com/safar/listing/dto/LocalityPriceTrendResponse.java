package com.safar.listing.dto;

import java.time.LocalDate;

public record LocalityPriceTrendResponse(
        String city,
        String locality,
        LocalDate month,
        Long avgPricePerSqftPaise,
        Long medianPricePerSqftPaise,
        Integer totalListings,
        Integer totalSold,
        String propertyType
) {}
