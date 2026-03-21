package com.safar.listing.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BulkAvailabilityRequest(
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotNull Boolean isAvailable,
        Long priceOverridePaise,
        Integer minStayNights,
        Integer maxStayNights
) {}
