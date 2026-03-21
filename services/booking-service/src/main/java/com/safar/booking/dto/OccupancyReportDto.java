package com.safar.booking.dto;

import java.util.List;
import java.util.UUID;

public record OccupancyReportDto(
        Double overallOccupancyPercent,
        Long adrPaise,
        Long revparPaise,
        Long totalRevenuePaise,
        Integer totalBookings,
        Integer totalNights,
        List<ListingOccupancy> listings,
        List<MonthlyBreakdown> monthlyBreakdown
) {
    public record ListingOccupancy(
            UUID listingId,
            String listingTitle,
            Double occupancyPercent,
            Long revenuePaise,
            Integer bookedNights,
            Integer totalAvailableNights,
            Integer bookingCount
    ) {}

    public record MonthlyBreakdown(
            String month,
            Double occupancyPercent,
            Long revenuePaise,
            Integer bookingCount
    ) {}
}
