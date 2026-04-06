package com.safar.listing.dto;

import com.safar.listing.entity.enums.LegalCaseStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LegalCaseResponse(
        UUID id,
        UUID userId,
        UUID advocateId,
        String advocateName,
        String packageType,
        UUID propertyId,
        String propertyAddress,
        String propertyCity,
        String propertyState,
        String surveyNumber,
        LegalCaseStatus status,
        String reportUrl,
        String remarks,
        Long feePaise,
        Boolean paid,
        Integer documentsCount,
        Integer verificationsComplete,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
