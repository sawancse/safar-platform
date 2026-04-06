package com.safar.listing.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MilestoneResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        Integer sequenceNumber,
        LocalDate scheduledDate,
        LocalDate completedDate,
        String status,
        Long paymentAmountPaise,
        Boolean paymentCollected,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
