package com.safar.listing.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AvailabilityResponse(
        UUID id,
        UUID listingId,
        LocalDate date,
        Boolean isAvailable,
        Long priceOverridePaise,
        Integer minStayNights,
        Integer maxStayNights
) {}
