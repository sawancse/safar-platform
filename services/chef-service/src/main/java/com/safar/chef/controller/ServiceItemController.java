package com.safar.chef.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.chef.dto.ServiceItemRequest;
import com.safar.chef.dto.ServiceItemResponse;
import com.safar.chef.entity.ServiceItem;
import com.safar.chef.service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Service items live under a listing. Vendors create/edit/delete items they
 * own; everyone else can read items for VERIFIED listings.
 *
 * Routes:
 *   GET    /api/v1/services/listings/{listingId}/items         (public; returns ACTIVE only)
 *   GET    /api/v1/services/listings/{listingId}/items/all     (vendor-owned; ACTIVE + PAUSED)
 *   POST   /api/v1/services/listings/{listingId}/items         (vendor-owned)
 *   GET    /api/v1/services/items/{itemId}                     (public)
 *   PATCH  /api/v1/services/items/{itemId}                     (vendor-owned)
 *   POST   /api/v1/services/items/{itemId}/pause | /activate   (vendor-owned)
 *   DELETE /api/v1/services/items/{itemId}                     (vendor-owned)
 */
@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceItemController {

    private final ServiceItemService itemService;
    private final ObjectMapper objectMapper;

    private UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    @GetMapping("/listings/{listingId}/items")
    public ResponseEntity<List<ServiceItemResponse>> listActive(@PathVariable UUID listingId) {
        return ResponseEntity.ok(itemService.listForListing(listingId, true).stream()
                .map(i -> ServiceItemResponse.from(i, objectMapper)).toList());
    }

    @GetMapping("/listings/{listingId}/items/all")
    public ResponseEntity<List<ServiceItemResponse>> listAll(@PathVariable UUID listingId, Authentication auth) {
        // Auth required — vendor sees their own paused + active items
        return ResponseEntity.ok(itemService.listForListing(listingId, false).stream()
                .map(i -> ServiceItemResponse.from(i, objectMapper)).toList());
    }

    @PostMapping("/listings/{listingId}/items")
    public ResponseEntity<ServiceItemResponse> create(@PathVariable UUID listingId,
                                                      @RequestBody ServiceItemRequest req,
                                                      Authentication auth) {
        ServiceItem created = itemService.create(listingId, req, userId(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceItemResponse.from(created, objectMapper));
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<ServiceItemResponse> get(@PathVariable UUID itemId) {
        return ResponseEntity.ok(ServiceItemResponse.from(itemService.get(itemId), objectMapper));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<ServiceItemResponse> update(@PathVariable UUID itemId,
                                                      @RequestBody ServiceItemRequest req,
                                                      Authentication auth) {
        return ResponseEntity.ok(ServiceItemResponse.from(
                itemService.update(itemId, req, userId(auth)), objectMapper));
    }

    @PostMapping("/items/{itemId}/pause")
    public ResponseEntity<ServiceItemResponse> pause(@PathVariable UUID itemId, Authentication auth) {
        return ResponseEntity.ok(ServiceItemResponse.from(
                itemService.setStatus(itemId, "PAUSED", userId(auth)), objectMapper));
    }

    @PostMapping("/items/{itemId}/activate")
    public ResponseEntity<ServiceItemResponse> activate(@PathVariable UUID itemId, Authentication auth) {
        return ResponseEntity.ok(ServiceItemResponse.from(
                itemService.setStatus(itemId, "ACTIVE", userId(auth)), objectMapper));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable UUID itemId, Authentication auth) {
        itemService.delete(itemId, userId(auth));
        return ResponseEntity.noContent().build();
    }
}
