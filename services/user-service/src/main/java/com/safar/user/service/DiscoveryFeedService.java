package com.safar.user.service;

import com.safar.user.dto.DiscoveryFeedResponse;
import com.safar.user.dto.DiscoveryFeedSection;
import com.safar.user.entity.DiscoveryFeedLog;
import com.safar.user.repository.DiscoveryFeedLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscoveryFeedService {

    private static final String ALGORITHM = "stub-v1";

    private final DiscoveryFeedLogRepository feedLogRepository;

    public DiscoveryFeedResponse generateFeed(UUID guestId, String city) {
        // Stub listing IDs for each section
        List<UUID> trendingIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        List<UUID> pickedIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        List<UUID> remoteIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        List<DiscoveryFeedSection> sections = List.of(
                new DiscoveryFeedSection("Trending This Weekend", trendingIds),
                new DiscoveryFeedSection("Picked For You", pickedIds),
                new DiscoveryFeedSection("Remote Work Certified", remoteIds)
        );

        // Collect all listing IDs for logging
        String allListingIds = sections.stream()
                .flatMap(s -> s.listingIds().stream())
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        // Log the feed generation
        DiscoveryFeedLog log = DiscoveryFeedLog.builder()
                .guestId(guestId)
                .listingIds(allListingIds)
                .algorithm(ALGORITHM)
                .build();
        feedLogRepository.save(log);

        return new DiscoveryFeedResponse(sections, "en");
    }
}
