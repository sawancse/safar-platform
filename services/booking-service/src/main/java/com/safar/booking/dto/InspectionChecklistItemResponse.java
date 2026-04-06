package com.safar.booking.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InspectionChecklistItemResponse(
        UUID id,
        String area,
        String itemName,
        String condition,
        String damageDescription,
        String photoUrls,
        long deductionPaise,
        OffsetDateTime createdAt
) {}
