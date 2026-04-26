package com.safar.chef.controller;

import com.safar.chef.dto.RejectListingRequest;
import com.safar.chef.dto.ServiceListingResponse;
import com.safar.chef.entity.enums.ServiceListingStatus;
import com.safar.chef.service.ServiceListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin queue for service-listing approvals. The admin only ever clicks
 * Approve / Reject — never types vendor data.
 */
@RestController
@RequestMapping("/api/v1/services/admin/listings")
@RequiredArgsConstructor
public class AdminServiceListingController {

    private final ServiceListingService listingService;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }

    private UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    @GetMapping
    public ResponseEntity<List<ServiceListingResponse>> queue(
            Authentication auth,
            @RequestParam(defaultValue = "PENDING_REVIEW") ServiceListingStatus status) {
        requireAdmin(auth);
        return ResponseEntity.ok(listingService.listByStatus(status).stream()
                .map(ServiceListingResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceListingResponse> get(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(ServiceListingResponse.from(listingService.get(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ServiceListingResponse> approve(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(ServiceListingResponse.from(
                listingService.approve(id, userId(auth))));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ServiceListingResponse> reject(@PathVariable UUID id,
                                                         @RequestBody RejectListingRequest body,
                                                         Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(ServiceListingResponse.from(
                listingService.reject(id, userId(auth), body.reason())));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ServiceListingResponse> suspend(@PathVariable UUID id,
                                                          @RequestBody RejectListingRequest body,
                                                          Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(ServiceListingResponse.from(
                listingService.suspend(id, userId(auth), body.reason())));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ServiceListingResponse> restore(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(ServiceListingResponse.from(
                listingService.restore(id, userId(auth))));
    }

    /**
     * Per-vendor commission override (negotiated deal — e.g. high-volume vendor
     * gets a flat 8% regardless of tier, or a strategic vendor gets 0% for the
     * launch period). Pass {@code commissionPctOverride: null} to clear and
     * fall back to the rate-config table.
     */
    public record CommissionOverrideRequest(
            java.math.BigDecimal commissionPctOverride,
            String commissionOverrideReason) {}

    @PutMapping("/{id}/commission-override")
    public ResponseEntity<ServiceListingResponse> setCommissionOverride(
            @PathVariable UUID id,
            @RequestBody CommissionOverrideRequest body,
            Authentication auth) {
        requireAdmin(auth);
        var listing = listingService.get(id);
        if (body.commissionPctOverride() != null) {
            if (body.commissionPctOverride().signum() < 0
                    || body.commissionPctOverride().compareTo(new java.math.BigDecimal("50")) > 0) {
                throw new IllegalArgumentException("commissionPctOverride must be in [0, 50]");
            }
        }
        listing.setCommissionPctOverride(body.commissionPctOverride());
        listing.setCommissionOverrideReason(body.commissionOverrideReason());
        // Bypass the lifecycle service — admin override is a metadata edit
        return ResponseEntity.ok(ServiceListingResponse.from(
                listingService.adminSaveListing(listing)));
    }
}
