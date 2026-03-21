package com.safar.user.controller;

import com.safar.user.dto.HostSubscriptionDto;
import com.safar.user.dto.SyncProfileRequest;
import com.safar.user.entity.enums.SubscriptionStatus;
import com.safar.user.entity.enums.SubscriptionTier;
import com.safar.user.service.HostSubscriptionService;
import com.safar.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final HostSubscriptionService hostSubscriptionService;
    private final ProfileService profileService;

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
            return ResponseEntity.ok(java.util.Map.of("email", email, "name", name));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("email", "", "name", ""));
        }
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
