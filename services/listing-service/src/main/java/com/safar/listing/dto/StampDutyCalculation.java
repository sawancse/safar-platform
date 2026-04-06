package com.safar.listing.dto;

public record StampDutyCalculation(
        String state,
        String agreementType,
        Long propertyValuePaise,
        Long stampDutyPaise,
        Long registrationFeePaise,
        Long surcharge,
        Long totalPaise
) {}
