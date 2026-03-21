package com.safar.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NomadCommentResponse(
        UUID id,
        UUID postId,
        UUID authorId,
        String authorName,
        String body,
        OffsetDateTime createdAt
) {}
