package com.safar.search.controller;

import com.safar.search.document.ListingDocument;
import com.safar.search.document.SalePropertyDocument;
import com.safar.search.dto.SalePropertySearchRequest;
import com.safar.search.dto.SaleSearchResponse;
import com.safar.search.dto.SearchHitsResponse;
import com.safar.search.dto.SearchRequest;
import com.safar.search.document.ExperienceDocument;
import com.safar.search.service.BuilderProjectSearchService;
import com.safar.search.service.ExperienceSearchService;
import com.safar.search.service.ListingServiceClient;
import com.safar.search.service.SalePropertySearchService;
import com.safar.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SalePropertySearchService saleSearchService;
    private final ExperienceSearchService experienceSearchService;
    private final BuilderProjectSearchService builderSearchService;
    private final ListingServiceClient listingClient;

    @org.springframework.beans.factory.annotation.Value("${services.listing-service.url}")
    private String listingServiceUrl;

    @org.springframework.beans.factory.annotation.Value("${elasticsearch.index.shards:1}")
    private int shards;

    @org.springframework.beans.factory.annotation.Value("${elasticsearch.index.replicas:0}")
    private int replicas;

    @GetMapping("/listings")
    public ResponseEntity<SearchHitsResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) List<String> type,
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Boolean instantBook,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Boolean petFriendly,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer minBathrooms,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) Integer starRating,
            @RequestParam(required = false) Boolean freeCancellation,
            @RequestParam(required = false) Boolean noPrepayment,
            @RequestParam(required = false) String cancellationPolicy,
            @RequestParam(required = false) String mealPlan,
            @RequestParam(required = false) List<String> bedTypes,
            @RequestParam(required = false) List<String> accessibilityFeatures,
            @RequestParam(required = false) Boolean medicalStay,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String procedure,
            @RequestParam(required = false) Boolean aashrayReady,
            @RequestParam(required = false) String occupancyType,
            @RequestParam(required = false) String foodType,
            @RequestParam(required = false) Boolean frontDesk24h,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String principal) {
        UUID userId = null;
        if (principal != null && !"anonymousUser".equals(principal)) {
            try { userId = UUID.fromString(principal); } catch (IllegalArgumentException ignored) {}
        }
        SearchRequest req = new SearchRequest(query, city, type, priceMin, priceMax,
                lat, lng, radiusKm, sort, instantBook, minRating,
                petFriendly, minBedrooms, minBathrooms, amenities,
                starRating, freeCancellation, noPrepayment,
                cancellationPolicy, mealPlan, bedTypes, accessibilityFeatures,
                medicalStay, specialty, procedure, aashrayReady,
                occupancyType, foodType, frontDesk24h,
                page, size);
        return ResponseEntity.ok(searchService.search(req, userId));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String q) {
        return ResponseEntity.ok(searchService.autocomplete(q));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<ListingDocument>> trending(@RequestParam String city) {
        return ResponseEntity.ok(searchService.getTrending(city));
    }

    /**
     * Recreate ES index from scratch (fixes mapping issues like geo_point).
     * Use when geo search returns wrong results.
     */
    @PostMapping("/recreate-index")
    public ResponseEntity<String> recreateIndex() {
        try {
            var indexOps = searchService.getIndexOps();
            if (indexOps.exists()) {
                indexOps.delete();
            }
            indexOps.create(org.springframework.data.elasticsearch.core.document.Document.from(
                    java.util.Map.of("index.number_of_shards", shards, "index.number_of_replicas", replicas)));
            indexOps.putMapping(indexOps.createMapping(com.safar.search.document.ListingDocument.class));
            log.info("Recreated 'listings' index with proper geo_point mapping");
            return ResponseEntity.ok("Index recreated — run /reindex-all next");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/reindex/{listingId}")
    public ResponseEntity<String> reindex(@PathVariable UUID listingId) {
        ListingDocument doc = listingClient.getListingDocument(listingId);
        if (doc == null) return ResponseEntity.notFound().build();
        doc.setIsVerified(true);
        doc.setIndexedAt(java.time.LocalDateTime.now());
        searchService.indexListing(doc);
        return ResponseEntity.ok("Indexed " + listingId);
    }

    @PostMapping("/reindex-all")
    public ResponseEntity<String> reindexAll() {
        org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
        int totalCount = 0;
        int pageNum = 0;
        try {
            while (true) {
                String url = listingServiceUrl + "/api/v1/listings?size=100&page=" + pageNum;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> page = rt.getForObject(url, java.util.Map.class);
                if (page == null) break;
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, Object>> content =
                        (java.util.List<java.util.Map<String, Object>>) page.get("content");
                if (content == null || content.isEmpty()) break;

                for (var listing : content) {
                    String id = (String) listing.get("id");
                    try {
                        ListingDocument doc = listingClient.getListingDocument(UUID.fromString(id));
                        if (doc != null) {
                            doc.setIsVerified(true);
                            doc.setIndexedAt(java.time.LocalDateTime.now());
                            searchService.indexListing(doc);
                            totalCount++;
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to index listing {}: {}", id, ex.getMessage());
                    }
                }

                Boolean last = (Boolean) page.get("last");
                if (Boolean.TRUE.equals(last)) break;
                pageNum++;
            }
            log.info("Reindex complete: {} listings across {} pages", totalCount, pageNum + 1);
            return ResponseEntity.ok("Reindexed " + totalCount + " listings across " + (pageNum + 1) + " pages");
        } catch (Exception e) {
            log.error("Reindex failed at page {}: {}", pageNum, e.getMessage());
            return ResponseEntity.internalServerError().body("Reindex failed: " + e.getMessage());
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<ListingDocument>> nearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(searchService.getNearby(lat, lng, radiusKm, limit, type));
    }

    // ── Sale Property Search ─────────────────────────────────────

    @GetMapping("/sale-properties")
    public ResponseEntity<SaleSearchResponse> searchSaleProperties(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) List<String> salePropertyType,
            @RequestParam(required = false) List<String> transactionType,
            @RequestParam(required = false) List<String> sellerType,
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            @RequestParam(required = false) List<Integer> bedrooms,
            @RequestParam(required = false) Integer minArea,
            @RequestParam(required = false) Integer maxArea,
            @RequestParam(required = false) String possessionStatus,
            @RequestParam(required = false) String furnishing,
            @RequestParam(required = false) List<String> facing,
            @RequestParam(required = false) Integer minFloor,
            @RequestParam(required = false) Integer maxFloor,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Boolean reraVerified,
            @RequestParam(required = false) Boolean vastuCompliant,
            @RequestParam(required = false) Boolean gatedCommunity,
            @RequestParam(required = false) Boolean petAllowed,
            @RequestParam(required = false) Boolean cornerProperty,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        var req = new SalePropertySearchRequest(query, city, locality, salePropertyType,
                transactionType, sellerType, priceMin, priceMax, bedrooms,
                minArea, maxArea, possessionStatus, furnishing, facing,
                minFloor, maxFloor, maxAge, reraVerified, vastuCompliant,
                gatedCommunity, petAllowed, cornerProperty, verified, amenities,
                lat, lng, radiusKm, sort, page, size);

        return ResponseEntity.ok(saleSearchService.search(req));
    }

    @GetMapping("/sale-properties/autocomplete")
    public ResponseEntity<List<SalePropertyDocument>> saleAutocomplete(@RequestParam String q) {
        return ResponseEntity.ok(saleSearchService.autocomplete(q));
    }

    @GetMapping("/sale-properties/recent")
    public ResponseEntity<List<SalePropertyDocument>> saleRecent(
            @RequestParam String city,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(saleSearchService.getRecentByCity(city, limit));
    }

    // ── Builder Projects Search ──────────────────────────────

    @GetMapping("/builder-projects")
    public ResponseEntity<Map<String, Object>> searchBuilderProjects(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) String projectStatus,
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            @RequestParam(required = false) List<Integer> bhk,
            @RequestParam(required = false) Boolean reraVerified,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(builderSearchService.search(query, city, locality,
                projectStatus, priceMin, priceMax, bhk, reraVerified,
                lat, lng, radiusKm, sort, page, size));
    }

    @GetMapping("/builder-projects/autocomplete")
    public ResponseEntity<List<Map<String, String>>> builderProjectAutocomplete(@RequestParam String q) {
        return ResponseEntity.ok(builderSearchService.autocomplete(q));
    }

    // ── Experience Search ──

    @GetMapping("/experiences")
    public ResponseEntity<Map<String, Object>> searchExperiences(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(experienceSearchService.search(query, city, category,
                priceMin, priceMax, lat, lng, radiusKm, sort, page, size));
    }

    @GetMapping("/experiences/autocomplete")
    public ResponseEntity<List<ExperienceDocument>> experienceAutocomplete(
            @RequestParam String q) {
        return ResponseEntity.ok(experienceSearchService.autocomplete(q));
    }

    @GetMapping("/experiences/trending")
    public ResponseEntity<List<ExperienceDocument>> experienceTrending(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(experienceSearchService.trending(city, limit));
    }
}
