package com.safar.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record LiveAnywhereBookRequest(
        @NotNull UUID listingId,
        @Positive int nights,
        @Positive long listingPricePerNightPaise
) {}
