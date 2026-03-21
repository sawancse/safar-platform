package com.safar.listing.controller;

import com.safar.listing.dto.RwCertRequest;
import com.safar.listing.dto.RwCertResponse;
import com.safar.listing.service.RwCertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings/{listingId}/rw-certification")
@RequiredArgsConstructor
public class RwCertificationController {

    private final RwCertificationService rwCertificationService;

    @PostMapping
    public ResponseEntity<RwCertResponse> apply(Authentication auth,
                                                 @PathVariable UUID listingId,
                                                 @RequestBody RwCertRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rwCertificationService.apply(hostId, listingId, req));
    }

    @GetMapping
    public ResponseEntity<RwCertResponse> getStatus(@PathVariable UUID listingId) {
        return ResponseEntity.ok(rwCertificationService.getByListingId(listingId));
    }
}
