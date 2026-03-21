package com.safar.listing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ExperienceBookingRequest(
        @NotNull UUID sessionId,
        @Positive int numGuests,
        UUID propertyBookingId
) {}
