package com.safar.messaging.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String content,
        String messageType,
        // Attachment
        String attachmentUrl,
        String attachmentName,
        Long attachmentSize,
        String attachmentType,
        // Location
        Double latitude,
        Double longitude,
        String locationLabel,
        // Read/time
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}
