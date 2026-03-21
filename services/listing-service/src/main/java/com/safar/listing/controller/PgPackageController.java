package com.safar.listing.controller;

import com.safar.listing.dto.PgPackageRequest;
import com.safar.listing.dto.PgPackageResponse;
import com.safar.listing.service.PgPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings/{listingId}/packages")
@RequiredArgsConstructor
public class PgPackageController {

    private final PgPackageService packageService;

    @PostMapping
    public ResponseEntity<PgPackageResponse> create(
            @PathVariable UUID listingId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PgPackageRequest req) {
        return ResponseEntity.ok(packageService.createPackage(listingId, userId, req));
    }

    @PutMapping("/{packageId}")
    public ResponseEntity<PgPackageResponse> update(
            @PathVariable UUID listingId,
            @PathVariable UUID packageId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PgPackageRequest req) {
        return ResponseEntity.ok(packageService.updatePackage(listingId, packageId, userId, req));
    }

    @DeleteMapping("/{packageId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID listingId,
            @PathVariable UUID packageId,
            @RequestHeader("X-User-Id") UUID userId) {
        packageService.deletePackage(listingId, packageId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PgPackageResponse>> list(@PathVariable UUID listingId) {
        return ResponseEntity.ok(packageService.getPackages(listingId));
    }
}
