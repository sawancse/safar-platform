package com.safar.listing.dto;

public record OccupancyRateDto(
        String city,
        String propertyType,
        double occupancyPct,
        double avgDailyRatePaise
) {}
