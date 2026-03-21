package com.safar.listing.controller;

import com.safar.listing.dto.MarketplaceAppResponse;
import com.safar.listing.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/marketplace")
@RequiredArgsConstructor
public class AdminMarketplaceController {

    private final MarketplaceService marketplaceService;

    @PutMapping("/apps/{id}/approve")
    public ResponseEntity<MarketplaceAppResponse> approveApp(
            Authentication auth,
            @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(marketplaceService.approveApp(id));
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
