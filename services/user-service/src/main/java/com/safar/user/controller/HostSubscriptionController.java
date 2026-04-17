package com.safar.user.controller;

import com.safar.user.dto.ActivateSubscriptionResponse;
import com.safar.user.dto.HostSubscriptionDto;
import com.safar.user.entity.enums.SubscriptionTier;
import com.safar.user.service.HostSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/subscription")
@RequiredArgsConstructor
public class HostSubscriptionController {

    private final HostSubscriptionService hostSubscriptionService;

    @GetMapping
    public ResponseEntity<HostSubscriptionDto> getSubscription(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        try {
            return ResponseEntity.ok(hostSubscriptionService.getSubscription(hostId));
        } catch (java.util.NoSuchElementException e) {
            // Return empty 200 instead of 404 — frontend handles null subscription as STARTER tier
            return ResponseEntity.ok(null);
        }
    }

    @PostMapping("/trial")
    public ResponseEntity<HostSubscriptionDto> startTrial(Authentication auth,
                                                           @RequestParam SubscriptionTier tier) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(hostSubscriptionService.startTrial(hostId, tier));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<HostSubscriptionDto> upgrade(Authentication auth,
                                                        @RequestParam SubscriptionTier tier) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(hostSubscriptionService.upgradeTier(hostId, tier));
    }

    @PostMapping("/activate")
    public ResponseEntity<ActivateSubscriptionResponse> activate(Authentication auth,
                                                                  @RequestParam SubscriptionTier tier) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(hostSubscriptionService.activate(hostId, tier));
    }
}
