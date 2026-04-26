package com.safar.services.controller;

import com.safar.services.dto.CreateChefProfileRequest;
import com.safar.services.dto.UpdateChefProfileRequest;
import com.safar.services.entity.ChefBooking;
import com.safar.services.entity.ChefProfile;
import com.safar.services.entity.EventBooking;
import com.safar.services.repository.ChefBookingRepository;
import com.safar.services.repository.EventBookingRepository;
import com.safar.services.service.ChefProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/chefs")
@RequiredArgsConstructor
public class ChefProfileController {

    private final ChefProfileService chefProfileService;
    private final ChefBookingRepository bookingRepository;
    private final EventBookingRepository eventRepository;

    /** Public reviews feed for a chef profile — concatenates rated bookings + events. */
    @GetMapping("/{chefId}/reviews")
    public ResponseEntity<List<Map<String, Object>>> getReviews(@PathVariable UUID chefId) {
        Stream<Map<String, Object>> bookingReviews = bookingRepository.findByChefId(chefId).stream()
                .filter(b -> b.getRatingGiven() != null)
                .map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", "bk-" + b.getId());
                    m.put("type", "BOOKING");
                    m.put("customerName", b.getCustomerName());
                    m.put("rating", b.getRatingGiven());
                    m.put("comment", b.getReviewComment());
                    m.put("serviceDate", b.getServiceDate());
                    m.put("createdAt", b.getUpdatedAt());
                    return m;
                });
        Stream<Map<String, Object>> eventReviews = eventRepository.findByChefId(chefId).stream()
                .filter(e -> e.getRatingGiven() != null)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", "ev-" + e.getId());
                    m.put("type", "EVENT");
                    m.put("customerName", e.getCustomerName());
                    m.put("rating", e.getRatingGiven());
                    m.put("comment", e.getReviewComment());
                    m.put("serviceDate", e.getEventDate());
                    m.put("createdAt", e.getUpdatedAt());
                    return m;
                });
        List<Map<String, Object>> combined = Stream.concat(bookingReviews, eventReviews)
                .sorted(Comparator.comparing(
                        (Map<String, Object> m) -> (java.time.OffsetDateTime) m.get("createdAt"),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return ResponseEntity.ok(combined);
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        }
    }

    @GetMapping
    public ResponseEntity<Page<ChefProfile>> browseChefs(
            @RequestParam(required = false) String city,
            Pageable pageable) {
        return ResponseEntity.ok(chefProfileService.browseChefs(city, pageable));
    }

    @PostMapping
    public ResponseEntity<ChefProfile> registerChef(Authentication auth,
                                                     @RequestBody CreateChefProfileRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chefProfileService.registerChef(userId, req));
    }

    @PutMapping("/me")
    public ResponseEntity<ChefProfile> updateProfile(Authentication auth,
                                                      @RequestBody UpdateChefProfileRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefProfileService.updateProfile(userId, req));
    }

    @GetMapping("/me")
    public ResponseEntity<ChefProfile> getMyProfile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefProfileService.getProfileByUserId(userId));
    }

    /** Claim an existing chef profile whose phone/email matches the current user's auth identity. */
    @PostMapping("/claim")
    public ResponseEntity<ChefProfile> claimProfile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefProfileService.claimByIdentity(userId));
    }

    @GetMapping("/{chefId}")
    public ResponseEntity<ChefProfile> getProfile(@PathVariable UUID chefId) {
        return ResponseEntity.ok(chefProfileService.getProfile(chefId));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ChefProfile>> searchChefs(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) String chefType,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chefProfileService.searchChefs(
                city, cuisine, locality, mealType, chefType, minRating, maxPrice, page, size));
    }

    @PutMapping("/me/availability")
    public ResponseEntity<ChefProfile> toggleAvailability(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefProfileService.toggleAvailability(userId));
    }

    // ── Admin ─────────────────────────────────────────────────

    @PostMapping("/admin/{chefId}/verify")
    public ResponseEntity<ChefProfile> adminVerify(@PathVariable UUID chefId, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(chefProfileService.verifyChef(chefId));
    }

    @PostMapping("/admin/{chefId}/reject")
    public ResponseEntity<ChefProfile> adminReject(@PathVariable UUID chefId,
                                                    @RequestParam(required = false) String reason,
                                                    Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(chefProfileService.rejectChef(chefId, reason));
    }

    @PostMapping("/admin/{chefId}/suspend")
    public ResponseEntity<ChefProfile> adminSuspend(@PathVariable UUID chefId, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(chefProfileService.suspendChef(chefId));
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<Page<ChefProfile>> getPendingChefs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(chefProfileService.getPendingChefs(page, size));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<Page<ChefProfile>> getAllChefs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(chefProfileService.getAllChefs(page, size));
    }
}
