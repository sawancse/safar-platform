package com.safar.user.controller;

import com.safar.user.dto.MedicalProfileRequest;
import com.safar.user.dto.MedicalProfileResponse;
import com.safar.user.service.MedicalProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MedicalProfileController {

    private final MedicalProfileService medicalProfileService;

    // ── Authenticated endpoints ────────────────────────────────────────────────

    @GetMapping("/api/v1/users/medical-profile")
    public ResponseEntity<MedicalProfileResponse> getMyMedicalProfile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return medicalProfileService.getProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/api/v1/users/medical-profile")
    public ResponseEntity<MedicalProfileResponse> createOrUpdateMedicalProfile(
            Authentication auth,
            @RequestBody MedicalProfileRequest request) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(medicalProfileService.createOrUpdate(userId, request));
    }

    @DeleteMapping("/api/v1/users/medical-profile")
    public ResponseEntity<Void> deleteMedicalProfile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        medicalProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    // ── Internal endpoint (no auth required) ───────────────────────────────────

    @GetMapping("/api/v1/internal/users/{userId}/medical-profile")
    public ResponseEntity<MedicalProfileResponse> getMedicalProfileInternal(@PathVariable UUID userId) {
        return medicalProfileService.getProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
