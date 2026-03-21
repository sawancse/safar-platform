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
        OffsetDateTime createdAt
) {}
