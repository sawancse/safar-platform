package com.safar.listing.controller;

import com.safar.listing.entity.SeekerProfile;
import com.safar.listing.entity.enums.ProfileStatus;
import com.safar.listing.service.SeekerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seekers")
@RequiredArgsConstructor
public class SeekerProfileController {

    private final SeekerProfileService seekerService;

    @PostMapping
    public ResponseEntity<SeekerProfile> createProfile(@RequestBody SeekerProfile profile) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seekerService.createProfile(profile));
    }

    @GetMapping
    public ResponseEntity<Page<SeekerProfile>> getProfiles(
            @RequestParam(required = false) ProfileStatus status,
            @RequestParam(required = false) String city,
            Pageable pageable) {
        return ResponseEntity.ok(seekerService.getProfiles(status, city, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeekerProfile> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(seekerService.getProfile(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeekerProfile> updateProfile(@PathVariable UUID id, @RequestBody SeekerProfile update) {
        return ResponseEntity.ok(seekerService.updateProfile(id, update));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateProfile(@PathVariable UUID id) {
        seekerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<SeekerProfileService.SeekerMatchResult>> findMatches(@PathVariable UUID id) {
        return ResponseEntity.ok(seekerService.findMatchingListings(id));
    }

    @GetMapping("/for-listing/{listingId}")
    public ResponseEntity<List<SeekerProfile>> findSeekersForListing(@PathVariable UUID listingId) {
        return ResponseEntity.ok(seekerService.findSeekersForListing(listingId));
    }
}
