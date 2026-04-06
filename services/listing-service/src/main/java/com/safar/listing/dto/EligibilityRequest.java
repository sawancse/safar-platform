package com.safar.listing.dto;

public record EligibilityRequest(
        String employmentType,
        Long monthlyIncomePaise,
        Long currentEmisPaise,
        Long desiredLoanAmountPaise,
        Integer desiredTenureMonths
) {}
