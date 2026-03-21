package com.safar.listing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppInstallationResponse(
        UUID id,
        UUID appId,
        UUID hostId,
        String scopesGranted,
        String accessToken,
        OffsetDateTime expiresAt,
        OffsetDateTime installedAt
) {}
