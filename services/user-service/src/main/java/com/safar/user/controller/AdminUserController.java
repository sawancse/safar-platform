package com.safar.user.controller;

import com.safar.user.dto.AdminHostDto;
import com.safar.user.entity.CohostProfile;
import com.safar.user.entity.NurtureCampaign;
import com.safar.user.entity.UserLead;
import com.safar.user.entity.UserProfile;
import com.safar.user.repository.NurtureCampaignRepository;
import com.safar.user.repository.ProfileRepository;
import com.safar.user.repository.UserLeadRepository;
import com.safar.user.service.CohostService;
import com.safar.user.service.LeadManagementService;
import com.safar.user.service.ProfileService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final ProfileService profileService;
    private final CohostService cohostService;
    private final ProfileRepository profileRepository;
    private final UserLeadRepository leadRepository;
    private final NurtureCampaignRepository nurtureCampaignRepository;
    private final LeadManagementService leadManagementService;
    private final RestTemplate restTemplate;

    @Value("${services.listing-service.url}")
    private String listingServiceUrl;

    @Value("${services.booking-service.url}")
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

    /** Suspend a host — all their listings get suspended too */
    @PostMapping("/hosts/{hostId}/suspend")
    public ResponseEntity<Map<String, Object>> suspendHost(
            Authentication auth,
            @PathVariable UUID hostId,
            @RequestBody Map<String, String> body) {
        requireAdmin(auth);
        UUID adminId = UUID.fromString(auth.getName());
        String reason = body.getOrDefault("reason", "Policy violation");
        profileService.suspendHost(hostId, adminId, reason);

        // Suspend all host's listings via listing-service
        int suspendedListings = 0;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(
                    listingServiceUrl + "/api/v1/internal/listings/by-host/" + hostId + "/suspend-all?reason=HOST_SUSPENDED&note=" +
                    java.net.URLEncoder.encode(reason, "UTF-8"), null, Map.class);
            if (result != null) suspendedListings = ((Number) result.getOrDefault("count", 0)).intValue();
        } catch (Exception e) {
            log.warn("Could not suspend host listings: {}", e.getMessage());
        }

        log.info("Host {} suspended by admin {}: {} ({} listings suspended)", hostId, adminId, reason, suspendedListings);
        return ResponseEntity.ok(Map.of(
                "status", "SUSPENDED",
                "hostId", hostId.toString(),
                "reason", reason,
                "listingsSuspended", suspendedListings
        ));
    }

    /** Unsuspend (reactivate) a host */
    @PostMapping("/hosts/{hostId}/unsuspend")
    public ResponseEntity<Map<String, Object>> unsuspendHost(
            Authentication auth, @PathVariable UUID hostId) {
        requireAdmin(auth);
        profileService.unsuspendHost(hostId);
        log.info("Host {} reactivated by admin {}", hostId, auth.getName());
        return ResponseEntity.ok(Map.of("status", "ACTIVE", "hostId", hostId.toString()));
    }

    /** Ban a host permanently */
    @PostMapping("/hosts/{hostId}/ban")
    public ResponseEntity<Map<String, Object>> banHost(
            Authentication auth,
            @PathVariable UUID hostId,
            @RequestBody Map<String, String> body) {
        requireAdmin(auth);
        UUID adminId = UUID.fromString(auth.getName());
        String reason = body.getOrDefault("reason", "Permanent ban");
        profileService.banHost(hostId, adminId, reason);

        // Suspend all listings
        try {
            restTemplate.postForObject(
                    listingServiceUrl + "/api/v1/internal/listings/by-host/" + hostId + "/suspend-all?reason=HOST_BANNED&note=" +
                    java.net.URLEncoder.encode(reason, "UTF-8"), null, Map.class);
        } catch (Exception e) {
            log.warn("Could not suspend banned host listings: {}", e.getMessage());
        }

        log.info("Host {} BANNED by admin {}: {}", hostId, adminId, reason);
        return ResponseEntity.ok(Map.of("status", "BANNED", "hostId", hostId.toString(), "reason", reason));
    }

    /** Get suspended/banned hosts */
    @GetMapping("/hosts/suspended")
    public ResponseEntity<List<AdminHostDto>> getSuspendedHosts(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(profileService.getSuspendedHosts());
    }

    @PutMapping("/cohost/{profileId}/verify")
    public ResponseEntity<CohostProfile> verifyCohost(Authentication auth,
                                                        @PathVariable UUID profileId) {
        requireAdmin(auth);
        return ResponseEntity.ok(cohostService.verifyProfile(profileId));
    }

    // ── All Users (paginated, filterable) ──────────────────

    @GetMapping("/users")
    public ResponseEntity<Page<Map<String, Object>>> getAllUsers(
            Authentication auth,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Pageable pageable) {
        requireAdmin(auth);

        Specification<UserProfile> spec = Specification.where(null);

        if (role != null && !role.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("accountStatus"), status));
        }
        if (search != null && !search.isBlank()) {
            String s = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), s),
                    cb.like(cb.lower(root.get("email")), s),
                    cb.like(root.get("phone"), "%" + search + "%")
            ));
        }
        try {
            if (dateFrom != null && !dateFrom.isBlank()) {
                OffsetDateTime dt = LocalDate.parse(dateFrom).atStartOfDay().atOffset(ZoneOffset.UTC);
                spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), dt));
            }
            if (dateTo != null && !dateTo.isBlank()) {
                OffsetDateTime dt = LocalDate.parse(dateTo).plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
                spec = spec.and((root, q, cb) -> cb.lessThan(root.get("createdAt"), dt));
            }
        } catch (Exception e) {
            log.warn("Invalid date filter: {} {}", dateFrom, dateTo);
        }

        Page<Map<String, Object>> result = profileRepository.findAll(spec, pageable)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", p.getUserId());
                    m.put("name", p.getName());
                    m.put("email", p.getEmail());
                    m.put("phone", p.getPhone());
                    m.put("role", p.getRole());
                    m.put("accountStatus", p.getAccountStatus() != null ? p.getAccountStatus() : "ACTIVE");
                    m.put("verificationLevel", p.getVerificationLevel());
                    m.put("loyaltyTier", p.getLoyaltyTier());
                    m.put("starHost", p.getStarHost());
                    m.put("profileCompletion", p.getProfileCompletion());
                    m.put("completedStays", p.getCompletedStays());
                    m.put("lastActiveAt", p.getLastActiveAt());
                    m.put("createdAt", p.getCreatedAt());
                    return m;
                });

        return ResponseEntity.ok(result);
    }

    // ── User Stats for Dashboard ─────────────────────────

    @GetMapping("/users/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(Authentication auth) {
        requireAdmin(auth);

        long total = profileRepository.count();
        OffsetDateTime now = OffsetDateTime.now();
        long thisWeek = profileRepository.countByCreatedAtAfter(now.minusDays(7));
        long thisMonth = profileRepository.countByCreatedAtAfter(now.minusDays(30));

        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Object[] row : profileRepository.countByRoleGrouped()) {
            byRole.put((String) row[0], (Long) row[1]);
        }

        long totalLeads = leadRepository.count();
        long leadsThisWeek = leadRepository.countByCreatedAtAfter(now.minusDays(7));
        long convertedLeads = leadRepository.countByConvertedTrue();

        return ResponseEntity.ok(Map.of(
                "totalUsers", total,
                "newThisWeek", thisWeek,
                "newThisMonth", thisMonth,
                "byRole", byRole,
                "totalLeads", totalLeads,
                "leadsThisWeek", leadsThisWeek,
                "convertedLeads", convertedLeads
        ));
    }

    // ── Leads Management (enhanced) ────────────────────────────

    @GetMapping("/leads")
    public ResponseEntity<Page<UserLead>> getLeads(
            Authentication auth,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String segment,
            @RequestParam(defaultValue = "score") String sortBy,
            Pageable pageable) {
        requireAdmin(auth);
        if (city != null && !city.isBlank()) {
            return ResponseEntity.ok(leadRepository.findByCityIgnoreCaseOrderByCreatedAtDesc(city, pageable));
        }
        if ("score".equals(sortBy)) {
            return ResponseEntity.ok(leadRepository.findAllByOrderByLeadScoreDesc(pageable));
        }
        return ResponseEntity.ok(leadRepository.findAllByOrderByCreatedAtDesc(pageable));
    }

    @GetMapping("/leads/stats")
    public ResponseEntity<Map<String, Object>> getLeadStats(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(leadManagementService.getLeadStats());
    }

    @GetMapping("/leads/campaigns")
    public ResponseEntity<List<NurtureCampaign>> getCampaigns(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(nurtureCampaignRepository.findAll());
    }

    @PostMapping("/leads/campaigns/{id}/toggle")
    public ResponseEntity<NurtureCampaign> toggleCampaign(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        NurtureCampaign c = nurtureCampaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        c.setActive(!c.getActive());
        return ResponseEntity.ok(nurtureCampaignRepository.save(c));
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
