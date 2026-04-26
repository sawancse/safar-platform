package com.safar.services.controller;

import com.safar.services.entity.VendorInvite;
import com.safar.services.service.VendorInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoints for BD agents to issue vendor invites + track funnel.
 *
 * Routes:
 *   POST   /api/v1/services/admin/invites
 *          body: { phone, serviceType, businessName?, notes?, sentVia? }
 *          returns: invite + deepLink + whatsAppMessage (ready to copy-paste)
 *   GET    /api/v1/services/admin/invites
 *   GET    /api/v1/services/admin/invites?serviceType=CAKE_DESIGNER
 *   POST   /api/v1/services/admin/invites/{id}/cancel
 */
@RestController
@RequestMapping("/api/v1/services/admin/invites")
@RequiredArgsConstructor
public class AdminVendorInviteController {

    private final VendorInviteService inviteService;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }

    public record CreateInvite(String phone, String serviceType, String businessName,
                               String notes, String sentVia) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(Authentication auth, @RequestBody CreateInvite req) {
        requireAdmin(auth);
        VendorInvite invite = inviteService.create(
                req.phone(), req.serviceType(), req.businessName(), req.notes(),
                req.sentVia(), UUID.fromString(auth.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "invite", invite,
                "deepLink", inviteService.buildDeepLink(invite),
                "whatsAppMessage", inviteService.buildWhatsAppMessage(invite)
        ));
    }

    @GetMapping
    public ResponseEntity<List<VendorInvite>> list(Authentication auth,
                                                   @RequestParam(required = false) String serviceType) {
        requireAdmin(auth);
        return ResponseEntity.ok(serviceType != null
                ? inviteService.listByServiceType(serviceType)
                : inviteService.listAll());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<VendorInvite> cancel(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(inviteService.cancel(id));
    }
}
