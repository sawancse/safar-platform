package com.safar.listing.controller;

import com.safar.listing.dto.AdminReviewRequest;
import com.safar.listing.dto.RwCertResponse;
import com.safar.listing.service.RwCertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/rw-certifications")
@RequiredArgsConstructor
public class AdminRwController {

    private final RwCertificationService rwCertificationService;

    @PutMapping("/{certId}/review")
    public ResponseEntity<RwCertResponse> review(Authentication auth,
                                                  @PathVariable UUID certId,
                                                  @RequestBody AdminReviewRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(
                rwCertificationService.adminReview(certId, req.approve(), req.adminNote()));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<RwCertResponse>> pending(Authentication auth,
                                                         @PageableDefault(size = 20) Pageable pageable) {
        requireAdmin(auth);
        return ResponseEntity.ok(rwCertificationService.getPending(pageable));
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
