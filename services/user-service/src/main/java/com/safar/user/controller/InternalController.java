package com.safar.user.controller;

import com.safar.user.dto.HostSubscriptionDto;
import com.safar.user.dto.SyncProfileRequest;
import com.safar.user.entity.HostKyc;
import com.safar.user.entity.enums.SubscriptionStatus;
import com.safar.user.entity.enums.SubscriptionTier;
import com.safar.user.repository.HostKycRepository;
import com.safar.user.service.HostSubscriptionService;
import com.safar.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final HostSubscriptionService hostSubscriptionService;
    private final ProfileService profileService;
    private final HostKycRepository kycRepository;
    private final com.safar.user.repository.ProfileRepository profileRepository;

    @PostMapping("/users/{userId}/sync-profile")
    public ResponseEntity<Void> syncProfile(@PathVariable UUID userId,
                                             @RequestBody SyncProfileRequest req) {
        profileService.syncFromAuth(userId, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}/email")
    public ResponseEntity<java.util.Map<String, String>> getUserEmail(@PathVariable UUID userId) {
        try {
            var profile = profileService.getMyProfile(userId);
            String email = profile.email() != null ? profile.email() : "";
            String name = profile.name() != null ? profile.name() : "";
            String phone = profile.phone() != null ? profile.phone() : "";
            return ResponseEntity.ok(java.util.Map.of("email", email, "name", name, "phone", phone));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("email", "", "name", ""));
        }
    }

    @GetMapping("/users/{userId}/contact")
    public ResponseEntity<java.util.Map<String, String>> getUserContact(@PathVariable UUID userId) {
        try {
            var profile = profileService.getMyProfile(userId);
            return ResponseEntity.ok(java.util.Map.of(
                    "name", profile.name() != null ? profile.name() : "",
                    "phone", profile.phone() != null ? profile.phone() : "",
                    "email", profile.email() != null ? profile.email() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("name", "", "phone", "", "email", ""));
        }
    }

    @GetMapping("/hosts/{hostId}/kyc-status")
    public ResponseEntity<Map<String, Object>> getHostKycStatus(@PathVariable UUID hostId) {
        var kyc = kycRepository.findByUserId(hostId).orElse(null);
        var profile = profileService.getMyProfile(hostId);

        // Check account status from entity directly
        var profileEntity = profileRepository.findById(hostId).orElse(null);
        String accountStatus = profileEntity != null && profileEntity.getAccountStatus() != null
                ? profileEntity.getAccountStatus() : "ACTIVE";

        var base = new java.util.HashMap<String, Object>();
        base.put("hostName", profile.name() != null ? profile.name() : "");
        base.put("hostPhone", profile.phone() != null ? profile.phone() : "");
        base.put("hostEmail", profile.email() != null ? profile.email() : "");
        base.put("accountStatus", accountStatus);

        if (kyc == null) {
            base.put("status", "NOT_STARTED");
            base.put("verified", false);
            return ResponseEntity.ok(base);
        }

        base.put("status", kyc.getStatus().name());
        base.put("verified", kyc.getStatus().name().equals("VERIFIED"));
        base.put("fullLegalName", kyc.getFullLegalName() != null ? kyc.getFullLegalName() : "");
        base.put("aadhaarVerified", kyc.getAadhaarVerified() != null && kyc.getAadhaarVerified());
        base.put("panVerified", kyc.getPanVerified() != null && kyc.getPanVerified());
        base.put("bankVerified", kyc.getBankVerified() != null && kyc.getBankVerified());
        base.put("submittedAt", kyc.getSubmittedAt() != null ? kyc.getSubmittedAt().toString() : "");
        base.put("verifiedAt", kyc.getVerifiedAt() != null ? kyc.getVerifiedAt().toString() : "");
        return ResponseEntity.ok(base);
    }

    @GetMapping("/hosts/{hostId}/subscription")
    public ResponseEntity<HostSubscriptionDto> getSubscription(@PathVariable UUID hostId) {
        try {
            return ResponseEntity.ok(hostSubscriptionService.getSubscription(hostId));
        } catch (NoSuchElementException e) {
            // No subscription = default TRIAL/STARTER for listing-service checks
            return ResponseEntity.ok(new HostSubscriptionDto(
                    null, hostId, SubscriptionTier.STARTER,
                    SubscriptionStatus.TRIAL, null, "MONTHLY", 99900, null, null,
                    0, false
            ));
        }
    }
}
