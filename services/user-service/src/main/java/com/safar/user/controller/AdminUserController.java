package com.safar.user.controller;

import com.safar.user.dto.AdminHostDto;
import com.safar.user.entity.CohostProfile;
import com.safar.user.repository.ProfileRepository;
import com.safar.user.service.CohostService;
import com.safar.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final ProfileService profileService;
    private final CohostService cohostService;
    private final ProfileRepository profileRepository;
    private final RestTemplate restTemplate;

    @Value("${services.listing.url:http://localhost:8083}")
    private String listingServiceUrl;

    @Value("${services.booking.url:http://localhost:8095}")
    private String bookingServiceUrl;

    @GetMapping("/hosts")
    public ResponseEntity<List<AdminHostDto>> getHosts(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(profileService.getAllHostsForAdmin());
    }

    @GetMapping("/analytics/summary")
    public ResponseEntity<Map<String, Object>> analyticsSummary(Authentication auth) {
        requireAdmin(auth);

        // Local counts from user-service
        long totalProfiles = profileRepository.count();
        long activeHosts = profileRepository.findAll().stream()
                .filter(p -> "HOST".equals(p.getRole()) || "BOTH".equals(p.getRole()) || "ADMIN".equals(p.getRole()))
                .count();
        long activeGuests = totalProfiles - activeHosts;

        // Fetch listing counts from listing-service
        long totalListings = 0;
        long pendingListings = 0;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = restTemplate.getForObject(
                    listingServiceUrl + "/api/v1/internal/listings/stats", Map.class);
            if (stats != null) {
                totalListings = ((Number) stats.getOrDefault("total", 0)).longValue();
                pendingListings = ((Number) stats.getOrDefault("pending", 0)).longValue();
            }
        } catch (Exception e) {
            log.warn("Could not fetch listing stats: {}", e.getMessage());
        }

        // Fetch booking count from booking-service
        long totalBookings = 0;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> bookingStats = restTemplate.getForObject(
                    bookingServiceUrl + "/api/v1/internal/bookings/stats", Map.class);
            if (bookingStats != null) {
                totalBookings = ((Number) bookingStats.getOrDefault("count", 0)).longValue();
            }
        } catch (Exception e) {
            log.warn("Could not fetch booking stats: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "totalListings", totalListings,
                "pendingListings", pendingListings,
                "totalBookings", totalBookings,
                "totalRevenuePaise", 0,
                "activeHosts", activeHosts,
                "activeGuests", activeGuests
        ));
    }

    @PutMapping("/cohost/{profileId}/verify")
    public ResponseEntity<CohostProfile> verifyCohost(Authentication auth,
                                                        @PathVariable UUID profileId) {
        requireAdmin(auth);
        return ResponseEntity.ok(cohostService.verifyProfile(profileId));
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
