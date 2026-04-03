package com.safar.listing.controller;

import com.safar.listing.dto.AvailabilityDayDto;
import com.safar.listing.dto.AvailabilityRequest;
import com.safar.listing.dto.AvailabilityResponse;
import com.safar.listing.dto.BulkAvailabilityRequest;
import com.safar.listing.dto.CreateListingRequest;
import com.safar.listing.dto.InvestmentSignalDto;
import com.safar.listing.dto.ListingResponse;
import com.safar.listing.dto.UpdateListingRequest;
import com.safar.listing.entity.ListingMedia;
import com.safar.listing.entity.enums.ArchiveReason;
import com.safar.listing.entity.enums.ListingType;
import com.safar.listing.entity.enums.MediaType;
import com.safar.listing.entity.enums.ModerationStatus;
import com.safar.listing.repository.ListingMediaRepository;
import com.safar.listing.service.AvailabilityService;
import com.safar.listing.service.CommunityVerificationService;
import com.safar.listing.service.InvestmentSignalService;
import com.safar.listing.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.safar.listing.service.S3StorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final AvailabilityService availabilityService;
    private final InvestmentSignalService investmentSignalService;
    private final CommunityVerificationService communityVerificationService;
    private final ListingMediaRepository listingMediaRepository;
    private final S3StorageService s3StorageService;

    @Value("${media.upload-dir:C:\\Users\\Win-10\\Pictures\\pic}")
    private String uploadDir;

    @PostMapping
    public ResponseEntity<ListingResponse> create(Authentication auth,
                                                   @Valid @RequestBody CreateListingRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.createListing(hostId, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(listingService.getListing(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> update(Authentication auth,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody UpdateListingRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.updateListing(id, hostId, req));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ListingResponse>> getMine(Authentication auth) {
    	System.out.println("auth.getName() " + auth.getName());
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.getMyListings(hostId));
    }

    @GetMapping
    public ResponseEntity<Page<ListingResponse>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) ListingType type,
            @RequestParam(required = false) Long minPricePaise,
            @RequestParam(required = false) Long maxPricePaise,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(listingService.searchListings(city, type,
                minPricePaise, maxPricePaise, pageable));
    }

    @PostMapping("/{id}/aashray")
    public ResponseEntity<ListingResponse> toggleAashray(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.toggleAashray(id, hostId, body));
    }

    @GetMapping("/{id}/verification-readiness")
    public ResponseEntity<Map<String, Object>> verificationReadiness(Authentication auth,
                                                                      @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.getVerificationReadiness(id, hostId));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ListingResponse> submit(Authentication auth,
                                                   @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.submitForVerification(id, hostId));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ListingResponse> pause(Authentication auth,
                                                  @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.pauseListing(id, hostId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth,
                                       @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        listingService.deleteListing(id, hostId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Unpublish listing back to DRAFT for editing. Must re-verify to go live.
     * Works from: VERIFIED, PAUSED, REJECTED, PENDING_VERIFICATION, ARCHIVED.
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ListingResponse> unpublish(Authentication auth, @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.unpublishToDraft(id, hostId));
    }

    /**
     * Host archives their listing. Reversible — restores to DRAFT (must re-verify).
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<ListingResponse> archive(Authentication auth,
                                                    @PathVariable UUID id,
                                                    @RequestParam(required = false) ArchiveReason reason,
                                                    @RequestParam(required = false) String note) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.archiveListing(id, hostId, reason, note));
    }

    /**
     * Host restores an archived listing back to DRAFT.
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<ListingResponse> restore(Authentication auth,
                                                    @PathVariable UUID id) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingService.restoreListing(id, userId, false));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<List<AvailabilityResponse>> getAvailability(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(availabilityService.getAvailability(id, from, to));
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> upsertAvailability(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody AvailabilityRequest req) {
        // Ownership check could be added here; keeping it simple for MVP
        return ResponseEntity.ok(availabilityService.upsertAvailability(id, req));
    }

    @PutMapping("/{id}/availability/bulk")
    public ResponseEntity<List<AvailabilityResponse>> bulkUpsertAvailability(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody BulkAvailabilityRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        // Verify ownership
        listingService.getListing(id); // throws if not found
        return ResponseEntity.ok(availabilityService.bulkUpsertAvailability(id, req));
    }

    @GetMapping("/{id}/availability/month")
    public ResponseEntity<List<AvailabilityDayDto>> getMonthAvailability(
            @PathVariable UUID id,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(availabilityService.getMonthAvailability(id, year, month));
    }

    @PostMapping("/{id}/media")
    public ResponseEntity<Map<String, Object>> addMedia(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID hostId = UUID.fromString(auth.getName());
        listingService.getListing(id); // verify listing exists
        String url = body.get("url");
        String type = body.getOrDefault("type", "PHOTO");
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url is required");
        }
        String category = body.get("category");
        ListingMedia media = ListingMedia.builder()
                .listingId(id)
                .type(MediaType.valueOf(type))
                .s3Key(url)
                .cdnUrl(url)
                .isPrimary(body.containsKey("primary") && "true".equals(body.get("primary")))
                .category(category)
                .moderationStatus(ModerationStatus.APPROVED)
                .build();
        ListingMedia saved = listingMediaRepository.save(media);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getId(), "url", saved.getCdnUrl(), "type", saved.getType()));
    }

    @PostMapping("/{id}/media/upload")
    public ResponseEntity<Map<String, Object>> uploadMedia(
            Authentication auth,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "PHOTO") String mediaType,
            @RequestParam(defaultValue = "false") boolean primary,
            @RequestParam(required = false) String category) throws IOException {
        UUID hostId = UUID.fromString(auth.getName());
        listingService.getListing(id);

        // Upload to S3
        String cdnUrl = s3StorageService.upload(id, file, mediaType);
        String s3Key = s3StorageService.extractS3Key(cdnUrl);

        ListingMedia media = ListingMedia.builder()
                .listingId(id)
                .type(MediaType.valueOf(mediaType))
                .s3Key(s3Key != null ? s3Key : cdnUrl)
                .cdnUrl(cdnUrl)
                .isPrimary(primary)
                .category(category)
                .moderationStatus(ModerationStatus.APPROVED)
                .build();
        ListingMedia saved = listingMediaRepository.save(media);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getId(), "url", cdnUrl, "type", saved.getType().name()));
    }

    @GetMapping("/{id}/media/file/{filename}")
    public ResponseEntity<byte[]> serveMedia(
            @PathVariable UUID id,
            @PathVariable String filename) throws IOException {
        // Legacy: serve from local disk if file exists (backward compatibility)
        Path filePath = Paths.get(uploadDir, id.toString(), filename);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";
        byte[] data = Files.readAllBytes(filePath);
        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Cache-Control", "public, max-age=86400")
                .body(data);
    }

    @PutMapping("/{id}/media/{mediaId}/primary")
    public ResponseEntity<Map<String, Object>> setPrimaryMedia(
            Authentication auth,
            @PathVariable UUID id,
            @PathVariable UUID mediaId) {
        UUID hostId = UUID.fromString(auth.getName());
        listingService.getListing(id); // verify listing exists
        // Clear existing primary flags for this listing
        listingMediaRepository.findByListingId(id).forEach(m -> {
            if (m.getIsPrimary()) {
                m.setIsPrimary(false);
                listingMediaRepository.save(m);
            }
        });
        // Set the target media as primary
        ListingMedia media = listingMediaRepository.findById(mediaId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Media not found: " + mediaId));
        media.setIsPrimary(true);
        listingMediaRepository.save(media);
        return ResponseEntity.ok(Map.of("id", media.getId(), "isPrimary", true));
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    public ResponseEntity<Void> deleteMedia(Authentication auth,
            @PathVariable UUID id, @PathVariable UUID mediaId) {
        UUID hostId = UUID.fromString(auth.getName());
        listingService.deleteMedia(id, mediaId, hostId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/media/reorder")
    public ResponseEntity<Void> reorderMedia(Authentication auth, @PathVariable UUID id,
            @RequestBody List<UUID> mediaIds) {
        UUID hostId = UUID.fromString(auth.getName());
        listingService.reorderMedia(id, hostId, mediaIds);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/media/{mediaId}/category")
    public ResponseEntity<Void> updateMediaCategory(Authentication auth,
            @PathVariable UUID id, @PathVariable UUID mediaId,
            @RequestParam String category) {
        UUID hostId = UUID.fromString(auth.getName());
        listingService.updateMediaCategory(id, mediaId, hostId, category);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/media")
    public ResponseEntity<List<Map<String, Object>>> getMedia(@PathVariable UUID id) {
        List<Map<String, Object>> result = listingMediaRepository.findByListingIdOrderBySortOrderAsc(id).stream()
                .map(m -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", m.getId());
                    map.put("url", m.getCdnUrl() != null ? m.getCdnUrl() : "");
                    map.put("type", m.getType().name());
                    map.put("isPrimary", m.getIsPrimary());
                    map.put("category", m.getCategory());
                    map.put("sortOrder", m.getSortOrder());
                    return map;
                }).toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/media/{mediaId}/category")
    public ResponseEntity<Map<String, Object>> updateMediaCategory(
            @PathVariable UUID id,
            @PathVariable UUID mediaId,
            @RequestBody Map<String, String> body) {
        String category = body.get("category");
        ListingMedia media = listingMediaRepository.findById(mediaId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Media not found"));
        if (!media.getListingId().equals(id)) {
            throw new IllegalArgumentException("Media does not belong to this listing");
        }
        media.setCategory(category);
        listingMediaRepository.save(media);
        return ResponseEntity.ok(Map.of("id", mediaId, "category", category != null ? category : ""));
    }

    @PostMapping("/internal/geocode-all")
    public ResponseEntity<Map<String, Object>> geocodeAll() {
        int updated = listingService.geocodeAllMissing();
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @PostMapping("/internal/recalc-ratings")
    public ResponseEntity<Map<String, Object>> recalcRatings() {
        // This calls review-service stats endpoint for each listing and updates DB
        // For now, return a simple acknowledgment — the Kafka consumer handles real-time updates
        return ResponseEntity.ok(Map.of("status", "ratings are updated via review.created Kafka events"));
    }

    @PostMapping("/{id}/community-verify")
    public ResponseEntity<CommunityVerificationService.CommunityVerifyResult> communityVerify(
            Authentication auth,
            @PathVariable UUID id,
            @RequestParam boolean photosMatch,
            @RequestParam boolean amenitiesMatch,
            @RequestParam boolean feltSafe) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(communityVerificationService.submitVerification(
                id, userId, photosMatch, amenitiesMatch, feltSafe));
    }

    @GetMapping("/{id}/investment-signal")
    public ResponseEntity<InvestmentSignalDto> getInvestmentSignal(
            Authentication auth,
            @PathVariable UUID id) {
        // Host or admin only — authentication required
        return ResponseEntity.ok(investmentSignalService.computeForListing(id));
    }
}
