package com.safar.supply.controller;

import com.safar.supply.dto.CreatePurchaseOrderRequest;
import com.safar.supply.dto.InvoicePoRequest;
import com.safar.supply.dto.PayPoRequest;
import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.PurchaseOrderItem;
import com.safar.supply.entity.enums.PurchaseOrderStatus;
import com.safar.supply.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders/admin")
@RequiredArgsConstructor
public class AdminPurchaseOrderController {

    private final PurchaseOrderService poService;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }

    private UUID userId(Authentication auth) {
        try { return UUID.fromString(auth.getName()); } catch (Exception e) { return null; }
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> list(Authentication auth,
                                                    @RequestParam(required = false) PurchaseOrderStatus status,
                                                    @RequestParam(required = false) UUID supplierId) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.list(status, supplierId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<PurchaseOrder>> overdue(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.overdue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> get(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.get(id));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<PurchaseOrderItem>> items(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.listItems(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> create(Authentication auth,
                                                @RequestBody CreatePurchaseOrderRequest req) {
        requireAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(poService.create(req, userId(auth)));
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<PurchaseOrder> issue(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.transition(id, PurchaseOrderStatus.ISSUED));
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<PurchaseOrder> ack(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.transition(id, PurchaseOrderStatus.ACKNOWLEDGED));
    }

    @PostMapping("/{id}/in-transit")
    public ResponseEntity<PurchaseOrder> inTransit(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.transition(id, PurchaseOrderStatus.IN_TRANSIT));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<PurchaseOrder> deliver(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.transition(id, PurchaseOrderStatus.DELIVERED));
    }

    @PostMapping("/{id}/invoice")
    public ResponseEntity<PurchaseOrder> invoice(Authentication auth, @PathVariable UUID id,
                                                  @RequestBody InvoicePoRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.invoice(id, req));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PurchaseOrder> pay(Authentication auth, @PathVariable UUID id,
                                              @RequestBody PayPoRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.pay(id, req));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrder> cancel(Authentication auth, @PathVariable UUID id,
                                                 @RequestParam(required = false) String reason) {
        requireAdmin(auth);
        return ResponseEntity.ok(poService.cancel(id, reason));
    }
}
