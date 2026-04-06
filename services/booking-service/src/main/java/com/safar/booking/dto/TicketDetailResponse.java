package com.safar.booking.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TicketDetailResponse(
        UUID id,
        UUID tenancyId,
        UUID listingId,
        String requestNumber,
        String category,
        String title,
        String description,
        String photoUrls,
        String priority,
        String status,
        String assignedTo,
        OffsetDateTime assignedAt,
        OffsetDateTime resolvedAt,
        String resolutionNotes,
        Integer tenantRating,
        String tenantFeedback,
        OffsetDateTime slaDeadlineAt,
        boolean slaBreached,
        int escalationLevel,
        OffsetDateTime escalatedAt,
        OffsetDateTime reopenedAt,
        int reopenCount,
        OffsetDateTime closedAt,
        List<TicketCommentResponse> comments,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
