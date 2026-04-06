package com.safar.booking.dto;

import java.util.Map;

public record TicketStatsResponse(
        long openCount,
        long inProgressCount,
        long resolvedCount,
        long closedCount,
        long slaBreachedCount,
        Double avgResolutionHours,
        double slaCompliancePercent,
        Map<String, Long> categoryBreakdown
) {}
