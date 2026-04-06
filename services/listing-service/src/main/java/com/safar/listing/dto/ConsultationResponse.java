package com.safar.listing.dto;

import com.safar.listing.entity.enums.ConsultationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationResponse(
        UUID id,
        UUID caseId,
        UUID advocateId,
        String advocateName,
        ConsultationStatus status,
        OffsetDateTime scheduledAt,
        Integer durationMinutes,
        String meetingLink,
        String notes,
        Long feePaise,
        Boolean paid,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
