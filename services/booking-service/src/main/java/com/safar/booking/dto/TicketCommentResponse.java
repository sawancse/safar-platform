package com.safar.booking.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketCommentResponse(
        UUID id,
        UUID authorId,
        String authorRole,
        String commentText,
        String attachmentUrls,
        boolean systemNote,
        OffsetDateTime createdAt
) {}
