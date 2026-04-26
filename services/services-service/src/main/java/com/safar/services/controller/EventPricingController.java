package com.safar.services.controller;

import com.safar.services.dto.CreateEventPricingDefaultRequest;
import com.safar.services.dto.EventPricingItemResponse;
import com.safar.services.dto.UpdateEventPricingRequest;
import com.safar.services.entity.ChefEventPricing;
import com.safar.services.entity.EventPricingDefault;
import com.safar.services.service.EventPricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chef-events/pricing")
@RequiredArgsConstructor
public class EventPricingController {

    private final EventPricingService pricingService;

    // ── Public ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<EventPricingItemResponse>> getPricing(
            @RequestParam(required = false) UUID chefId) {
        return ResponseEntity.ok(pricingService.getPricing(chefId));
    }

    // ── Chef (authenticated) ───────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<List<EventPricingItemResponse>> getMyPricing(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(pricingService.getChefPricing(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<List<ChefEventPricing>> updateMyPricing(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody List<UpdateEventPricingRequest> items) {
        return ResponseEntity.ok(pricingService.setChefPricingBulk(userId, items));
    }

    @PutMapping("/me/{itemKey}")
    public ResponseEntity<ChefEventPricing> updateMyItemPrice(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable String itemKey,
            @Valid @RequestBody UpdateEventPricingRequest req) {
        return ResponseEntity.ok(pricingService.setChefPrice(userId, req));
    }

    @DeleteMapping("/me/{itemKey}")
    public ResponseEntity<Void> resetMyItemPrice(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable String itemKey) {
        pricingService.resetChefPrice(userId, itemKey);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        }
    }

    // ── Admin ──────────────────────────────────────────────────────────────

    @GetMapping("/admin")
    public ResponseEntity<List<EventPricingDefault>> getAdminDefaults(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(pricingService.getAllDefaults());
    }

    @PostMapping("/admin")
    public ResponseEntity<EventPricingDefault> createDefault(
            @Valid @RequestBody CreateEventPricingDefaultRequest req, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingService.createDefault(req));
    }

    @PutMapping("/admin/{itemKey}")
    public ResponseEntity<EventPricingDefault> updateDefault(
            @PathVariable String itemKey,
            @Valid @RequestBody CreateEventPricingDefaultRequest req,
            Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(pricingService.updateDefault(itemKey, req));
    }

    @DeleteMapping("/admin/{itemKey}")
    public ResponseEntity<Void> deactivateDefault(@PathVariable String itemKey, Authentication auth) {
        requireAdmin(auth);
        pricingService.deactivateDefault(itemKey);
        return ResponseEntity.noContent().build();
    }
}
