package com.safar.listing.dto;

import java.util.UUID;

public record EligibilityResponse(
        UUID id,
        Long maxEligibleAmountPaise,
        Long maxEmiPaise,
        Long desiredLoanAmountPaise,
        Integer desiredTenureMonths,
        boolean eligible
) {}
