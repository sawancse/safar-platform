package com.safar.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BucketListItemDto(
        UUID id,
        UUID guestId,
        UUID listingId,
        OffsetDateTime addedAt,
        String notes,
        String listingTitle,
        String listingCity,
        String listingImageUrl
) {}
