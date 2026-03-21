package com.safar.listing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record VpnEnrollRequest(
        @Min(1) @Max(20) Integer commissionPct,
        @Min(1) Integer minStayNights,
        LocalDate availableFrom,
        LocalDate availableTo
) {}
