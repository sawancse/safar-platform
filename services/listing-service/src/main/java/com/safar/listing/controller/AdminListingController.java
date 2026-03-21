package com.safar.listing.controller;

import com.safar.listing.dto.ListingResponse;
import com.safar.listing.entity.enums.ArchiveReason;
import com.safar.listing.entity.enums.ListingStatus;
import com.safar.listing.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/listings")
@RequiredArgsConstructor
public class AdminListingController {

    private final ListingService listingService;

    @PutMapping("/{id}/verify")
    public ResponseEntity<ListingResponse> verify(Authentication auth,
                                                   @PathVariable UUID id,
                                                   @RequestParam(required = false) String notes) {
        requireAdmin(auth);
        return ResponseEntity.ok(listingService.verifyListing(id, notes));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ListingResponse> reject(Authentication auth,
                                                   @PathVariable UUID id,
                                                   @RequestBody(required = false) Map<String, String> body) {
        requireAdmin(auth);
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(listingService.rejectListing(id, notes));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ListingResponse>> pending(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(listingService.getListingsByStatus(ListingStatus.PENDING_VERIFICATION));
    }

    @GetMapping
    public ResponseEntity<List<ListingResponse>> listByStatus(
            Authentication auth,
            @RequestParam(required = false) ListingStatus status) {
        requireAdmin(auth);
        if (status != null) {
            return ResponseEntity.ok(listingService.getListingsByStatus(status));
        }
        // Return all listings across all statuses
        List<ListingResponse> all = new java.util.ArrayList<>();
        for (ListingStatus s : ListingStatus.values()) {
            all.addAll(listingService.getListingsByStatus(s));
        }
        return ResponseEntity.ok(all);
    }

    /**
     * Admin suspends a listing (fraud, policy violation, duplicate, etc.).
     * Host cannot restore — only admin can.
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<ListingResponse> suspend(Authentication auth,
                                                    @PathVariable UUID id,
                                                    @RequestParam ArchiveReason reason,
                                                    @RequestParam(required = false) String note) {
        requireAdmin(auth);
        UUID adminId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.suspendListing(id, adminId, reason, note));
    }

    /**
     * Admin restores a suspended or archived listing back to DRAFT.
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<ListingResponse> restore(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        UUID adminId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.restoreListing(id, adminId, true));
    }

    /**
     * List all archived + suspended listings.
     */
    @GetMapping("/archived")
    public ResponseEntity<List<ListingResponse>> archived(Authentication auth) {
        requireAdmin(auth);
        List<ListingResponse> result = new java.util.ArrayList<>();
        result.addAll(listingService.getListingsByStatus(ListingStatus.ARCHIVED));
        result.addAll(listingService.getListingsByStatus(ListingStatus.SUSPENDED));
        return ResponseEntity.ok(result);
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
