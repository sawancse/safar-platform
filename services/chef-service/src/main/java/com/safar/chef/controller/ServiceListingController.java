package com.safar.chef.controller;

import com.safar.chef.dto.*;
import com.safar.chef.entity.ServiceListing;
import com.safar.chef.entity.VendorKycDocument;
import com.safar.chef.service.KycDocumentService;
import com.safar.chef.service.ServiceListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Vendor-facing endpoints for managing their own service listings.
 *
 * Public: GET /api/v1/services/listings, GET /api/v1/services/listings/{slug}
 * Vendor: GET /api/v1/services/listings/me, lifecycle transitions
 *
 * Admin endpoints live in {@link AdminServiceListingController}.
 */
@RestController
@RequestMapping("/api/v1/services/listings")
@RequiredArgsConstructor
public class ServiceListingController {

    private final ServiceListingService listingService;
    private final KycDocumentService kycService;

    private UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ── Public (no auth) ────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ServiceListingResponse>> listVerified(
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate availableOn) {
        List<ServiceListing> listings = listingService.listVerified(serviceType, city, availableOn);
        return ResponseEntity.ok(listings.stream().map(ServiceListingResponse::from).toList());
    }

    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<ServiceListingResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ServiceListingResponse.from(listingService.getBySlug(slug)));
    }

    // ── Vendor-owned ────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<List<ServiceListingResponse>> listMine(Authentication auth) {
        return ResponseEntity.ok(listingService.listByVendor(userId(auth)).stream()
                .map(ServiceListingResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceListingResponse> get(@PathVariable UUID id, Authentication auth) {
        ServiceListing listing = listingService.get(id);
        // Public if VERIFIED; otherwise must be the owner
        if (!listing.getStatus().name().equals("VERIFIED")
                && (auth == null || !listing.getVendorUserId().equals(userId(auth)))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Listing not yet verified");
        }
        return ResponseEntity.ok(ServiceListingResponse.from(listing));
    }

    @PostMapping
    public ResponseEntity<ServiceListingResponse> create(@RequestBody CreateServiceListingRequest req,
                                                         Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceListingResponse.from(
                listingService.create(req, userId(auth))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceListingResponse> update(@PathVariable UUID id,
                                                         @RequestBody UpdateServiceListingRequest req,
                                                         Authentication auth) {
        return ResponseEntity.ok(ServiceListingResponse.from(
                listingService.update(id, req, userId(auth))));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ServiceListingResponse> submit(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(ServiceListingResponse.from(
                listingService.submit(id, userId(auth))));
    }

    // ── KYC documents ───────────────────────────────────────

    @PostMapping("/{id}/kyc-documents")
    public ResponseEntity<KycDocumentResponse> uploadKyc(@PathVariable UUID id,
                                                         @RequestBody UploadKycDocumentRequest req,
                                                         Authentication auth) {
        VendorKycDocument doc = kycService.upload(id, req, userId(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(KycDocumentResponse.from(doc));
    }

    @GetMapping("/{id}/kyc-documents")
    public ResponseEntity<List<KycDocumentResponse>> listKyc(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(kycService.listForListing(id, userId(auth), isAdmin(auth))
                .stream().map(KycDocumentResponse::from).toList());
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ServiceListingResponse> pause(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(ServiceListingResponse.from(
                listingService.pause(id, userId(auth))));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ServiceListingResponse> resume(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(ServiceListingResponse.from(
                listingService.resume(id, userId(auth))));
    }
}
