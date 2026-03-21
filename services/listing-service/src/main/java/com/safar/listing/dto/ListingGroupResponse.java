package com.safar.listing.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ListingGroupResponse(
        UUID id,
        UUID hostId,
        String name,
        Integer bundleDiscountPct,
        List<UUID> listingIds,
        OffsetDateTime createdAt
) {}
