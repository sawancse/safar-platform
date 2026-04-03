package com.safar.listing.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleVisitRequest(
        @NotNull UUID salePropertyId,
        UUID inquiryId,
        @NotNull OffsetDateTime scheduledAt,
        Integer durationMinutes
) {}
