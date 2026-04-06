package com.safar.listing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoomDesignResponse(
        UUID id,
        UUID projectId,
        String roomType,
        Integer areaSqft,
        String designStyle,
        String moodBoardUrl,
        String floorPlanUrl,
        String render3dUrl,
        String status,
        String remarks,
        Long estimatedCostPaise,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
