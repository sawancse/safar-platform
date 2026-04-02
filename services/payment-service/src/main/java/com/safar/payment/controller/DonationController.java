package com.safar.payment.controller;

import com.safar.payment.dto.*;
import com.safar.payment.entity.enums.DonationStatus;
import com.safar.payment.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    /**
     * Create a donation and get Razorpay order/subscription for payment.
     * Authenticated donors get their ID linked; anonymous donors pass null.
     */
    @PostMapping
    public ResponseEntity<DonationOrderResponse> createDonation(
            Authentication auth,
            @Valid @RequestBody CreateDonationRequest req) throws Exception {
        UUID donorId = auth != null ? UUID.fromString(auth.getName()) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.createDonation(donorId, req));
    }

    /**
     * Verify Razorpay payment signature after successful checkout.
     */
    @PostMapping("/verify")
    public ResponseEntity<DonationResponse> verifyDonation(
            @Valid @RequestBody VerifyDonationRequest req) {
        return ResponseEntity.ok(donationService.verifyDonation(req));
    }

    /**
     * Public stats for the donation page (progress bar, social proof, recent donors).
     */
    @GetMapping("/stats")
    public ResponseEntity<DonationStatsResponse> getStats() {
        return ResponseEntity.ok(donationService.getStats());
    }

    /**
     * Get a specific donation by reference.
     */
    @GetMapping("/{donationRef}")
    public ResponseEntity<DonationResponse> getDonation(@PathVariable String donationRef) {
        return ResponseEntity.ok(donationService.getDonation(donationRef));
    }

    /**
     * List authenticated donor's own donations.
     */
    @GetMapping("/my")
    public ResponseEntity<Page<DonationResponse>> getMyDonations(
            Authentication auth, Pageable pageable) {
        UUID donorId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(donationService.getMyDonations(donorId, pageable));
    }

    /**
     * Public leaderboard — top donors (anonymized) and cities.
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<DonorLeaderboardResponse> getLeaderboard() {
        return ResponseEntity.ok(donationService.getLeaderboard());
    }

    /**
     * Admin: list all donations with optional status filter.
     */
    @GetMapping("/admin")
    public ResponseEntity<Page<DonationResponse>> getAllDonations(
            @RequestParam(required = false) DonationStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(donationService.getAllDonations(status, pageable));
    }
}
