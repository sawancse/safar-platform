package com.safar.listing.dto;

public record DemandTrendDto(
        String city,
        String month,
        long searchVolume,
        long bookingCount,
        double avgOccupancyPct
) {}
