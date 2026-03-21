package com.safar.listing.dto;

public record InvestmentSignalDto(
        long avgMonthlyRevenuePaise,
        double annualYieldPct,
        double occupancyRatePct,
        String demandLevel,
        String trend,
        String confidenceLevel,
        int dataPointCount,
        String disclaimer
) {}
