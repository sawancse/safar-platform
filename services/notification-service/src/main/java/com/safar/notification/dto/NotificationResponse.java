package com.safar.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String body,
        String type,
        String referenceId,
        String referenceType,
        boolean read,
        OffsetDateTime createdAt
) {}
