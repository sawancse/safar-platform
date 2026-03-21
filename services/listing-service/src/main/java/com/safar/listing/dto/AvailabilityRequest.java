package com.safar.listing.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AvailabilityRequest(
        @NotNull LocalDate date,
        @NotNull Boolean isAvailable,
        Long priceOverridePaise,
        Integer minStayNights
) {}
