package com.safar.user.service;

import com.safar.user.dto.DiscoveryFeedResponse;
import com.safar.user.entity.DiscoveryFeedLog;
import com.safar.user.repository.DiscoveryFeedLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryFeedServiceTest {

    @Mock DiscoveryFeedLogRepository feedLogRepository;
    @InjectMocks DiscoveryFeedService discoveryFeedService;

    private final UUID GUEST_ID = UUID.randomUUID();

    @Test
    void generateFeed_returnsSectionsAndLogs() {
        when(feedLogRepository.save(any())).thenAnswer(inv -> {
            DiscoveryFeedLog log = inv.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        DiscoveryFeedResponse response = discoveryFeedService.generateFeed(GUEST_ID, "Mumbai");

        assertThat(response.sections()).hasSize(3);
        assertThat(response.sections().get(0).title()).isEqualTo("Trending This Weekend");
        assertThat(response.sections().get(1).title()).isEqualTo("Picked For You");
        assertThat(response.sections().get(2).title()).isEqualTo("Remote Work Certified");

        // Each section should have listing IDs
        response.sections().forEach(section ->
                assertThat(section.listingIds()).isNotEmpty()
        );

        // Verify the feed generation was logged
        verify(feedLogRepository).save(any(DiscoveryFeedLog.class));
    }
}
