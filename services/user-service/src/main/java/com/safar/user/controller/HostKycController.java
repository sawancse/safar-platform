package com.safar.user.controller;

import com.safar.user.dto.*;
import com.safar.user.service.HostKycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/kyc")
@RequiredArgsConstructor
public class HostKycController {

    private final HostKycService kycService;

    @GetMapping
    public ResponseEntity<HostKycDto> getKyc(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.getKyc(userId));
    }

    @PutMapping("/identity")
    public ResponseEntity<HostKycDto> updateIdentity(Authentication auth,
                                                      @Valid @RequestBody UpdateKycIdentityRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.updateIdentity(userId, req));
    }

    @PutMapping("/address")
    public ResponseEntity<HostKycDto> updateAddress(Authentication auth,
                                                     @Valid @RequestBody UpdateKycAddressRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.updateAddress(userId, req));
    }

    @PutMapping("/bank")
    public ResponseEntity<HostKycDto> updateBank(Authentication auth,
                                                  @Valid @RequestBody UpdateKycBankRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.updateBank(userId, req));
    }

    @PutMapping("/business")
    public ResponseEntity<HostKycDto> updateBusiness(Authentication auth,
                                                      @RequestBody UpdateKycBusinessRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.updateBusiness(userId, req));
    }

    @PostMapping("/submit")
    public ResponseEntity<HostKycDto> submit(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.submit(userId));
    }

    // Admin endpoints
    @GetMapping("/admin/pending")
    public ResponseEntity<List<HostKycDto>> getPending() {
        return ResponseEntity.ok(kycService.getPendingKycs());
    }

    @PostMapping("/admin/{kycId}/approve")
    public ResponseEntity<HostKycDto> approve(@PathVariable UUID kycId, Authentication auth) {
        UUID adminId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.approve(kycId, adminId));
    }

    @PostMapping("/admin/{kycId}/reject")
    public ResponseEntity<HostKycDto> reject(@PathVariable UUID kycId,
                                              @RequestBody java.util.Map<String, String> body,
                                              Authentication auth) {
        UUID adminId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.reject(kycId, adminId, body.get("reason")));
    }
}
