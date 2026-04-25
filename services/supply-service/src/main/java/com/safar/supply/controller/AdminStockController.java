package com.safar.supply.controller;

import com.safar.supply.dto.StockAdjustRequest;
import com.safar.supply.dto.StockItemRequest;
import com.safar.supply.entity.StockItem;
import com.safar.supply.entity.StockMovement;
import com.safar.supply.entity.enums.ItemCategory;
import com.safar.supply.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock/admin")
@RequiredArgsConstructor
public class AdminStockController {

    private final StockService stockService;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }

    @GetMapping
    public ResponseEntity<List<StockItem>> list(Authentication auth,
                                                @RequestParam(required = false) ItemCategory category,
                                                @RequestParam(defaultValue = "false") boolean lowOnly) {
        requireAdmin(auth);
        return ResponseEntity.ok(stockService.list(category, lowOnly));
    }

    @GetMapping("/items/{itemKey}")
    public ResponseEntity<StockItem> getByKey(Authentication auth, @PathVariable String itemKey) {
        requireAdmin(auth);
        return ResponseEntity.ok(stockService.getByKey(itemKey));
    }

    @GetMapping("/items/{itemKey}/movements")
    public ResponseEntity<List<StockMovement>> movements(Authentication auth, @PathVariable String itemKey) {
        requireAdmin(auth);
        StockItem item = stockService.getByKey(itemKey);
        return ResponseEntity.ok(stockService.movementsForItem(item.getId()));
    }

    @GetMapping("/movements")
    public ResponseEntity<List<StockMovement>> recentMovements(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(stockService.recentMovements());
    }

    @PostMapping("/items")
    public ResponseEntity<StockItem> upsertItem(Authentication auth, @RequestBody StockItemRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(stockService.upsertItem(req));
    }

    @PostMapping("/items/{itemKey}/adjust")
    public ResponseEntity<StockItem> adjust(Authentication auth, @PathVariable String itemKey,
                                            @RequestBody StockAdjustRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(stockService.adjust(itemKey, req));
    }
}
