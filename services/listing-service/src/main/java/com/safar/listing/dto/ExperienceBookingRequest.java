package com.safar.listing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record ExperienceBookingRequest(
        UUID sessionId,
        @NotNull UUID experienceId,
        @Positive int numGuests,
        UUID propertyBookingId,
        LocalDate requestedDate
) {}
