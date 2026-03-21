package com.safar.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicHostDto(
        UUID id,
        String name,
        String avatarUrl,
        String bio,
        String languages,
        String verificationLevel,
        Integer trustScore,
        String trustBadge,
        Integer responseRate,
        Integer avgResponseMinutes,
        Integer totalHostReviews,
        String hostType,
        Boolean selfieVerified,
        OffsetDateTime createdAt,
        OffsetDateTime lastActiveAt
) {}
