package com.safar.user.controller;

import com.safar.user.dto.HostKycDto;
import com.safar.user.dto.KycAdminDetailDto;
import com.safar.user.service.HostKycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private final HostKycService kycService;

    /**
     * Get all KYCs with optional status filter.
     * If status is provided, filter by status; otherwise return all non-NOT_STARTED.
     */
    @GetMapping
    public ResponseEntity<List<HostKycDto>> getAllKycs(
            @RequestParam(required = false) String status,
            Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(kycService.getAllKycs(status));
    }

    /**
     * Get full KYC details with trust score and fraud check info.
     */
    @GetMapping("/{kycId}")
    public ResponseEntity<KycAdminDetailDto> getKycDetail(
            @PathVariable UUID kycId,
            Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(kycService.getKycAdminDetail(kycId));
    }

    /**
     * Approve a single KYC.
     */
    @PostMapping("/{kycId}/approve")
    public ResponseEntity<HostKycDto> approve(@PathVariable UUID kycId, Authentication auth) {
        requireAdmin(auth);
        UUID adminId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.approve(kycId, adminId));
    }

    /**
     * Reject a single KYC.
     */
    @PostMapping("/{kycId}/reject")
    public ResponseEntity<HostKycDto> reject(@PathVariable UUID kycId,
                                              @RequestBody Map<String, String> body,
                                              Authentication auth) {
        requireAdmin(auth);
        UUID adminId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(kycService.reject(kycId, adminId, body.get("reason")));
    }

    /**
     * Bulk approve multiple KYCs at once.
     */
    @PostMapping("/bulk-approve")
    public ResponseEntity<Map<String, Object>> bulkApprove(
            @RequestBody List<UUID> kycIds,
            Authentication auth) {
        requireAdmin(auth);
        UUID adminId = UUID.fromString(auth.getName());
        List<HostKycDto> approved = kycService.bulkApprove(kycIds, adminId);
        return ResponseEntity.ok(Map.of(
                "approved", approved.size(),
                "results", approved
        ));
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
