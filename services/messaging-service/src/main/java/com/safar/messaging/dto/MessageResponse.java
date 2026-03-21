package com.safar.messaging.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String content,
        String messageType,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}
