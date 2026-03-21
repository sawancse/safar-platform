package com.safar.listing.controller;

import com.safar.listing.dto.ListingResponse;
import com.safar.listing.entity.ListingDraft;
import com.safar.listing.service.ListingDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingDraftController {

    private final ListingDraftService listingDraftService;

    @PostMapping("/generate-draft")
    public ResponseEntity<ListingDraft> generateDraft(
            Authentication auth,
            @RequestParam String address,
            @RequestParam String type) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingDraftService.generateDraft(hostId, address, type));
    }

    @PostMapping("/from-draft/{draftId}")
    public ResponseEntity<ListingResponse> convertToListing(
            Authentication auth,
            @PathVariable UUID draftId) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingDraftService.convertToListing(hostId, draftId));
    }

    @GetMapping("/drafts")
    public ResponseEntity<List<ListingDraft>> getDrafts(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(listingDraftService.getDrafts(hostId));
    }
}
