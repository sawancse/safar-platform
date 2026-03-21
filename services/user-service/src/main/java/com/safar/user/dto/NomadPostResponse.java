package com.safar.user.dto;

import com.safar.user.entity.enums.PostCategory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NomadPostResponse(
        UUID id,
        UUID authorId,
        String authorName,
        String title,
        String body,
        PostCategory category,
        String city,
        Integer upvotes,
        long commentCount,
        OffsetDateTime createdAt
) {}
