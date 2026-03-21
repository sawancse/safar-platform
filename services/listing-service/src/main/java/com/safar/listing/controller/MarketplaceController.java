package com.safar.listing.controller;

import com.safar.listing.dto.*;
import com.safar.listing.service.MarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @PostMapping("/apps")
    public ResponseEntity<MarketplaceAppResponse> registerApp(
            Authentication auth,
            @Valid @RequestBody RegisterAppRequest req) {
        UUID developerId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(marketplaceService.registerApp(developerId, req));
    }

    @GetMapping("/apps")
    public ResponseEntity<List<MarketplaceAppResponse>> getDeveloperApps(Authentication auth) {
        UUID developerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(marketplaceService.getDeveloperApps(developerId));
    }

    @GetMapping
    public ResponseEntity<Page<MarketplaceAppResponse>> getPublicApps(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(marketplaceService.getPublicApps(pageable));
    }

    @PostMapping("/apps/{id}/install")
    public ResponseEntity<AppInstallationResponse> installApp(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody InstallAppRequest req) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(marketplaceService.installApp(hostId, id, req));
    }
}
