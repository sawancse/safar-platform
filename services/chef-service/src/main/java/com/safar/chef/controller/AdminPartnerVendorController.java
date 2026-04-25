package com.safar.chef.controller;

import com.safar.chef.dto.PartnerVendorRequest;
import com.safar.chef.entity.PartnerVendor;
import com.safar.chef.entity.enums.VendorServiceType;
import com.safar.chef.service.PartnerVendorService;
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
 * Admin-only CRUD for partner vendors (cake designers, decorators, pandits,
 * singers, appliance providers, staff-hire vendors). Vendors do not self-serve
 * in this phase — admins onboard, KYC, and assign manually.
 */
@RestController
@RequestMapping("/api/v1/vendors/admin")
@RequiredArgsConstructor
public class AdminPartnerVendorController {

    private final PartnerVendorService vendorService;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }

    @GetMapping
    public ResponseEntity<List<PartnerVendor>> list(Authentication auth,
                                                    @RequestParam VendorServiceType serviceType,
                                                    @RequestParam(defaultValue = "false") boolean activeOnly) {
        requireAdmin(auth);
        return ResponseEntity.ok(vendorService.listByType(serviceType, activeOnly));
    }

    @GetMapping("/eligible")
    public ResponseEntity<List<PartnerVendor>> eligible(Authentication auth,
                                                        @RequestParam VendorServiceType serviceType,
                                                        @RequestParam(required = false) String city) {
        requireAdmin(auth);
        return ResponseEntity.ok(vendorService.findEligible(serviceType, city));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerVendor> get(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(vendorService.get(id));
    }

    @PostMapping
    public ResponseEntity<PartnerVendor> create(Authentication auth, @RequestBody PartnerVendorRequest req) {
        requireAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartnerVendor> update(Authentication auth, @PathVariable UUID id,
                                                @RequestBody PartnerVendorRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(vendorService.update(id, req));
    }

    @PostMapping("/{id}/active")
    public ResponseEntity<PartnerVendor> setActive(Authentication auth, @PathVariable UUID id,
                                                   @RequestParam boolean value) {
        requireAdmin(auth);
        return ResponseEntity.ok(vendorService.setActive(id, value));
    }

    @PostMapping("/{id}/kyc")
    public ResponseEntity<PartnerVendor> verifyKyc(Authentication auth, @PathVariable UUID id,
                                                   @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        boolean verified = Boolean.TRUE.equals(body.get("verified"));
        String notes = body.get("notes") == null ? null : body.get("notes").toString();
        return ResponseEntity.ok(vendorService.verifyKyc(id, verified, notes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        vendorService.setActive(id, false);
        return ResponseEntity.noContent().build();
    }
}
