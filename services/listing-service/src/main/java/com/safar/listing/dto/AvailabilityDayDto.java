package com.safar.listing.dto;

import java.time.LocalDate;

public record AvailabilityDayDto(
        LocalDate date,
        boolean isAvailable,
        Long priceOverridePaise,
        Integer minStayNights,
        Integer maxStayNights,
        boolean hasBooking
) {}
