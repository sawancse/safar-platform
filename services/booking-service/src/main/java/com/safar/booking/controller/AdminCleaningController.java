package com.safar.booking.controller;

import com.safar.booking.entity.CleanerProfile;
import com.safar.booking.service.CleaningNetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/cleaning")
@RequiredArgsConstructor
public class AdminCleaningController {

    private final CleaningNetworkService cleaningService;

    @PutMapping("/cleaners/{id}/verify")
    public ResponseEntity<CleanerProfile> verifyCleaner(Authentication auth,
                                                          @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(cleaningService.verifyCleaner(id));
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
