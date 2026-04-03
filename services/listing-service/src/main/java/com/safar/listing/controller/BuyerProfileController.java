package com.safar.listing.controller;

import com.safar.listing.dto.BuyerProfileRequest;
import com.safar.listing.dto.BuyerProfileResponse;
import com.safar.listing.dto.SalePropertyResponse;
import com.safar.listing.service.BuyerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buyer-profile")
@RequiredArgsConstructor
public class BuyerProfileController {

    private final BuyerProfileService buyerProfileService;

    @PostMapping
    public ResponseEntity<BuyerProfileResponse> createOrUpdate(
            @RequestBody BuyerProfileRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(buyerProfileService.createOrUpdate(request, userId));
    }

    @GetMapping
    public ResponseEntity<BuyerProfileResponse> getProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(buyerProfileService.getProfile(userId));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<SalePropertyResponse>> getRecommendations(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(buyerProfileService.getRecommendations(userId));
    }
}
