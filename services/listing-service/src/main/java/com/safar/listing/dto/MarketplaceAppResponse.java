package com.safar.listing.dto;

import com.safar.listing.entity.enums.AppStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MarketplaceAppResponse(
        UUID id,
        UUID developerId,
        String appName,
        String description,
        String clientId,
        String redirectUris,
        String scopes,
        AppStatus status,
        String webhookUrl,
        Integer rateLimitRpm,
        OffsetDateTime createdAt
) {}
