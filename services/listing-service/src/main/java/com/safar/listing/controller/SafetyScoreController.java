package com.safar.listing.controller;

import com.safar.listing.dto.SafetyScoreDto;
import com.safar.listing.entity.Listing;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.service.SafetyScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class SafetyScoreController {

    private final SafetyScoreService safetyScoreService;
    private final ListingRepository listingRepository;

    @GetMapping("/{id}/safety-score")
    public ResponseEntity<SafetyScoreDto> getSafetyScore(@PathVariable UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + id));
        SafetyScoreDto score = safetyScoreService.computeScore(id, listing.getCity());
        safetyScoreService.updateListingSafety(id, score);
        return ResponseEntity.ok(score);
    }
}
