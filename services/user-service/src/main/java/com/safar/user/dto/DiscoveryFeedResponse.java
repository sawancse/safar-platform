package com.safar.user.dto;

import java.util.List;

public record DiscoveryFeedResponse(
        List<DiscoveryFeedSection> sections,
        String language
) {}
