package com.safar.supply.controller;

import com.safar.supply.dto.ConsumeStockRequest;
import com.safar.supply.entity.StockMovement;
import com.safar.supply.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Service-to-service endpoint. Called by chef-service when a vendor delivers a
 * bespoke job (cake / decor / etc.) — debits the BOM from stock.
 *
 * Path /api/v1/internal/** is permit-all in SecurityConfig; gateway should NOT
 * expose this externally. (When/if a gateway route is added, mark it internal-only.)
 */
@RestController
@RequestMapping("/api/v1/internal/stock")
@RequiredArgsConstructor
public class InternalStockController {

    private final StockService stockService;

    @PostMapping("/consume")
    public ResponseEntity<List<StockMovement>> consume(@RequestBody ConsumeStockRequest req) {
        return ResponseEntity.ok(stockService.consume(req));
    }
}
