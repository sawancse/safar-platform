package com.safar.user.controller;

import com.safar.user.dto.CohostAgreementRequest;
import com.safar.user.dto.CohostProfileRequest;
import com.safar.user.entity.CohostAgreement;
import com.safar.user.entity.CohostEarnings;
import com.safar.user.entity.CohostProfile;
import com.safar.user.service.CohostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cohost")
@RequiredArgsConstructor
public class CohostController {

    private final CohostService cohostService;

    @PostMapping("/profile")
    public ResponseEntity<CohostProfile> createProfile(Authentication auth,
                                                         @Valid @RequestBody CohostProfileRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(cohostService.createProfile(hostId, req));
    }

    @PutMapping("/profile")
    public ResponseEntity<CohostProfile> updateProfile(Authentication auth,
                                                         @Valid @RequestBody CohostProfileRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(cohostService.updateProfile(hostId, req));
    }

    @GetMapping("/profiles")
    public ResponseEntity<Page<CohostProfile>> searchCohosts(@RequestParam String city,
                                                               Pageable pageable) {
        return ResponseEntity.ok(cohostService.searchCohosts(city, pageable));
    }

    @PostMapping("/listings/{listingId}/invite/{cohostId}")
    public ResponseEntity<CohostAgreement> inviteCohost(Authentication auth,
                                                          @PathVariable UUID listingId,
                                                          @PathVariable UUID cohostId,
                                                          @Valid @RequestBody CohostAgreementRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cohostService.createAgreement(hostId, listingId, cohostId, req));
    }

    @PutMapping("/agreements/{id}/accept")
    public ResponseEntity<CohostAgreement> acceptAgreement(Authentication auth,
                                                             @PathVariable UUID id) {
        UUID cohostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(cohostService.acceptAgreement(cohostId, id));
    }

    @GetMapping("/earnings")
    public ResponseEntity<List<CohostEarnings>> getEarnings(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(cohostService.getEarnings(hostId));
    }
}
