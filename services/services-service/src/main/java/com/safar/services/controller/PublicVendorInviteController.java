package com.safar.services.controller;

import com.safar.services.entity.VendorInvite;
import com.safar.services.service.VendorInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Public-but-token-gated endpoint the wizard hits when a vendor lands on
 * /vendor/onboard/{type}?invite={token}. Returns just enough to pre-fill
 * phone + business_name and stamps opened_at on the invite row.
 *
 * No authentication required — the token IS the auth (hashed so it's not
 * brute-forceable: 32 random bytes = 256 bits).
 */
@RestController
@RequestMapping("/api/v1/services/invites")
@RequiredArgsConstructor
public class PublicVendorInviteController {

    private final VendorInviteService inviteService;

    @GetMapping("/{token}")
    public ResponseEntity<?> resolve(@PathVariable String token) {
        Optional<VendorInvite> active = inviteService.markOpened(token)
                .filter(i -> i.getCancelledAt() == null)
                .filter(i -> i.getExpiresAt() == null
                        || i.getExpiresAt().isAfter(java.time.OffsetDateTime.now()));

        if (active.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "valid", false,
                    "message", "This invite link has expired or been cancelled. Ask your Safar contact for a new one."
            ));
        }

        VendorInvite invite = active.get();
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "phone", invite.getPhone(),
                "businessName", invite.getBusinessName() == null ? "" : invite.getBusinessName(),
                "serviceType", invite.getServiceType()
        ));
    }
}
