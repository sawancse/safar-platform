package com.safar.chef.controller;

import com.safar.chef.dto.CreateChefProfileRequest;
import com.safar.chef.dto.UpdateChefProfileRequest;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.service.ChefProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chefs")
@RequiredArgsConstructor
public class ChefProfileController {

    private final ChefProfileService chefProfileService;

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
