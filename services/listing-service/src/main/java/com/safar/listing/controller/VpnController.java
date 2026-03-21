package com.safar.listing.controller;

import com.safar.listing.dto.VpnEnrollRequest;
import com.safar.listing.entity.VpnListing;
import com.safar.listing.entity.VpnReferral;
import com.safar.listing.service.VacantPropertyNetworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VpnController {

    private final VacantPropertyNetworkService vpnService;

    @PostMapping("/listings/{id}/vpn/enroll")
    public ResponseEntity<VpnListing> enroll(Authentication auth,
                                              @PathVariable UUID id,
                                              @Valid @RequestBody VpnEnrollRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vpnService.enrollInNetwork(hostId, id, req));
    }

    @DeleteMapping("/listings/{id}/vpn")
    public ResponseEntity<Void> remove(Authentication auth,
                                        @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        vpnService.removeFromNetwork(hostId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/listings/{id}/vpn/referral-link")
    public ResponseEntity<Map<String, String>> generateReferralLink(Authentication auth,
                                                                     @PathVariable UUID id) {
        UUID referrerId = UUID.fromString(auth.getName());
        String code = vpnService.generateReferralLink(referrerId, id);
        return ResponseEntity.ok(Map.of("referralCode", code));
    }

    @GetMapping("/vpn/listings")
    public ResponseEntity<Page<VpnListing>> browseNetwork(
            @RequestParam String city,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(vpnService.findNetworkListings(city, pageable));
    }

    @GetMapping("/vpn/referrals")
    public ResponseEntity<Page<VpnReferral>> myReferrals(Authentication auth,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        UUID referrerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(vpnService.getReferralHistory(referrerId, pageable));
    }
}
