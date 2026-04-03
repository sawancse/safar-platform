package com.safar.listing.controller;

import com.safar.listing.dto.*;
import com.safar.listing.entity.enums.SalePropertyStatus;
import com.safar.listing.service.SalePropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sale-properties")
@RequiredArgsConstructor
public class SalePropertyController {

    private final SalePropertyService salePropertyService;

    @GetMapping
    public ResponseEntity<Page<SalePropertyResponse>> browse(
            @RequestParam(required = false) String city,
            Pageable pageable) {
        return ResponseEntity.ok(salePropertyService.browse(city, pageable));
    }

    @PostMapping
    public ResponseEntity<SalePropertyResponse> create(
            @Valid @RequestBody CreateSalePropertyRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salePropertyService.create(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalePropertyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(salePropertyService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalePropertyResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateSalePropertyRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(salePropertyService.update(id, request, userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SalePropertyResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam SalePropertyStatus status,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(salePropertyService.updateStatus(id, status, userId));
    }

    @GetMapping("/seller")
    public ResponseEntity<Page<SalePropertyResponse>> getSellerProperties(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) SalePropertyStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(salePropertyService.getSellerProperties(userId, status, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        salePropertyService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<SalePropertyResponse>> getSimilar(@PathVariable UUID id) {
        return ResponseEntity.ok(salePropertyService.getSimilarProperties(id));
    }

    // Admin endpoints
    @PostMapping("/{id}/verify")
    public ResponseEntity<SalePropertyResponse> adminVerify(@PathVariable UUID id) {
        return ResponseEntity.ok(salePropertyService.adminVerify(id));
    }

    @PostMapping("/{id}/verify-rera")
    public ResponseEntity<SalePropertyResponse> adminVerifyRera(@PathVariable UUID id) {
        return ResponseEntity.ok(salePropertyService.adminVerifyRera(id));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<SalePropertyResponse> adminSuspend(@PathVariable UUID id) {
        return ResponseEntity.ok(salePropertyService.adminSuspend(id));
    }

    @GetMapping("/admin/list")
    public ResponseEntity<Page<SalePropertyResponse>> adminList(
            @RequestParam(required = false) SalePropertyStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(salePropertyService.adminList(status, pageable));
    }

    @PostMapping("/admin/reindex")
    public ResponseEntity<Integer> adminReindex() {
        return ResponseEntity.ok(salePropertyService.reindexAll());
    }
}
