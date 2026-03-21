package com.safar.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateCleaningJobRequest(
        @NotNull UUID listingId,
        @NotNull OffsetDateTime scheduledAt,
        BigDecimal estimatedHours,
        String notes
) {}
