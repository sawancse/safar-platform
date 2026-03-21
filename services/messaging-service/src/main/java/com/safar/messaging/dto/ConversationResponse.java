package com.safar.messaging.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID participant1Id,
        UUID participant2Id,
        UUID listingId,
        UUID bookingId,
        String lastMessageText,
        OffsetDateTime lastMessageAt,
        int unreadCount,
        OffsetDateTime createdAt
) {}
