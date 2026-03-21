package com.safar.media.dto;

import java.util.UUID;

public record PresignResponse(
        UUID mediaId,
        String uploadUrl,
        String s3Key,
        String cdnUrl
) {}
