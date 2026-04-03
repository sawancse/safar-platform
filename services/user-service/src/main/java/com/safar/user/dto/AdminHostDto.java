package com.safar.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminHostDto(
        UUID id,
        String name,
        String phone,
        String email,
        String role,
        String subscriptionTier,
        String kycStatus,
        String accountStatus,
        String suspensionReason,
        OffsetDateTime suspendedAt,
        OffsetDateTime createdAt
) {}
