package com.safar.listing.dto;

public record RentalYieldDto(
        String city,
        double avgYieldPct,
        long avgMonthlyRentPaise,
        long avgPropertyValuePaise,
        String trend
) {}
