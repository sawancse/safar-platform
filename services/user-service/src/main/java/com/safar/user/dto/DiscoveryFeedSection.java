package com.safar.user.dto;

import java.util.List;
import java.util.UUID;

public record DiscoveryFeedSection(
        String title,
        List<UUID> listingIds
) {}
