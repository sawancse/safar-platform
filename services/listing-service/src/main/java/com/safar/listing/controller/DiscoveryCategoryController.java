package com.safar.listing.controller;

import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.DiscoveryCategory;
import com.safar.listing.service.DiscoveryCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discover")
@RequiredArgsConstructor
public class DiscoveryCategoryController {

    private final DiscoveryCategoryService service;

    /**
     * List all discovery categories with listing counts (public).
     * Powers the category carousel on homepage.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        return ResponseEntity.ok(service.getCategoriesWithCounts());
    }

    /**
     * Browse listings by discovery category (public).
     */
    @GetMapping("/categories/{category}")
    public ResponseEntity<List<Listing>> browseByCategory(
            @PathVariable DiscoveryCategory category, Pageable pageable) {
        return ResponseEntity.ok(service.getListingsByCategory(category, pageable));
    }

    /**
     * Set discovery categories for a listing (host, max 3).
     */
    @PutMapping("/listings/{listingId}/categories")
    public ResponseEntity<Map<String, String>> setCategories(
            Authentication auth,
            @PathVariable UUID listingId,
            @RequestBody List<DiscoveryCategory> categories) {
        UUID hostId = UUID.fromString(auth.getName());
        service.setCategories(listingId, hostId, categories);
        return ResponseEntity.ok(Map.of("status", "Categories updated"));
    }

    /**
     * Get AI-suggested categories for a listing (host).
     */
    @GetMapping("/listings/{listingId}/suggest-categories")
    public ResponseEntity<List<DiscoveryCategory>> suggestCategories(
            Authentication auth, @PathVariable UUID listingId) {
        // Would normally fetch listing — simplified for now
        return ResponseEntity.ok(List.of(
                DiscoveryCategory.WEEKEND_GETAWAYS,
                DiscoveryCategory.BUDGET_GEMS
        ));
    }
}
