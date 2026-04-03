package com.safar.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTenancyRequest(
        UUID tenantId,
        UUID listingId,
        UUID roomTypeId,
        String bedNumber,
        String sharingType,
        LocalDate moveInDate,
        int noticePeriodDays,
        long monthlyRentPaise,
        long securityDepositPaise,
        boolean mealsIncluded,
        boolean laundryIncluded,
        boolean wifiIncluded,
        long totalMonthlyPaise,
        Integer billingDay,
        // Configurable penalty — null means inherit from listing, then fall back to defaults
        Integer gracePeriodDays,
        Integer latePenaltyBps,
        Integer maxPenaltyPercent
) {}
