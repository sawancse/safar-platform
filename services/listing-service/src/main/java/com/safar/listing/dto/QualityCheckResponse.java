package com.safar.listing.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record QualityCheckResponse(
        UUID id,
        UUID projectId,
        UUID milestoneId,
        String checkType,
        String status,
        String inspectorName,
        List<String> photoUrls,
        String findings,
        Boolean passed,
        String remarks,
        OffsetDateTime inspectedAt,
        OffsetDateTime createdAt
) {}
