package com.safar.supply.controller;

import com.safar.supply.dto.CatalogItemRequest;
import com.safar.supply.dto.SupplierRequest;
import com.safar.supply.entity.Supplier;
import com.safar.supply.entity.SupplierCatalogItem;
import com.safar.supply.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers/admin")
@RequiredArgsConstructor
public class AdminSupplierController {

    private final SupplierService supplierService;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }

    @GetMapping
    public ResponseEntity<List<Supplier>> list(Authentication auth,
                                               @RequestParam(defaultValue = "false") boolean activeOnly) {
        requireAdmin(auth);
        return ResponseEntity.ok(supplierService.list(activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Supplier> get(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(supplierService.get(id));
    }

    @PostMapping
    public ResponseEntity<Supplier> create(Authentication auth, @RequestBody SupplierRequest req) {
        requireAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> update(Authentication auth, @PathVariable UUID id,
                                           @RequestBody SupplierRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(supplierService.update(id, req));
    }

    @PostMapping("/{id}/active")
    public ResponseEntity<Supplier> setActive(Authentication auth, @PathVariable UUID id,
                                              @RequestParam boolean value) {
        requireAdmin(auth);
        return ResponseEntity.ok(supplierService.setActive(id, value));
    }

    @PostMapping("/{id}/kyc")
    public ResponseEntity<Supplier> verifyKyc(Authentication auth, @PathVariable UUID id,
                                              @RequestBody Map<String, Object> body) {
        requireAdmin(auth);
        boolean verified = Boolean.TRUE.equals(body.get("verified"));
        String notes = body.get("notes") == null ? null : body.get("notes").toString();
        return ResponseEntity.ok(supplierService.verifyKyc(id, verified, notes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        supplierService.setActive(id, false);
        return ResponseEntity.noContent().build();
    }

    // ── Catalog ────────────────────────────────────────────────────

    @GetMapping("/{id}/catalog")
    public ResponseEntity<List<SupplierCatalogItem>> listCatalog(Authentication auth,
                                                                 @PathVariable UUID id,
                                                                 @RequestParam(defaultValue = "true") boolean activeOnly) {
        requireAdmin(auth);
        return ResponseEntity.ok(supplierService.listCatalog(id, activeOnly));
    }

    @PostMapping("/{id}/catalog")
    public ResponseEntity<SupplierCatalogItem> addCatalogItem(Authentication auth,
                                                              @PathVariable UUID id,
                                                              @RequestBody CatalogItemRequest req) {
        requireAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.addCatalogItem(id, req));
    }

    @PutMapping("/{id}/catalog/{itemId}")
    public ResponseEntity<SupplierCatalogItem> updateCatalogItem(Authentication auth,
                                                                  @PathVariable UUID id,
                                                                  @PathVariable UUID itemId,
                                                                  @RequestBody CatalogItemRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(supplierService.updateCatalogItem(id, itemId, req));
    }

    @DeleteMapping("/{id}/catalog/{itemId}")
    public ResponseEntity<Void> softDeleteCatalogItem(Authentication auth,
                                                      @PathVariable UUID id,
                                                      @PathVariable UUID itemId) {
        requireAdmin(auth);
        supplierService.softDeleteCatalogItem(id, itemId);
        return ResponseEntity.noContent().build();
    }
}
