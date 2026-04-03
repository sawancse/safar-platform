package com.safar.listing.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ConstructionUpdateResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        Integer progressPercent,
        List<String> photos,
        OffsetDateTime createdAt
) {}
