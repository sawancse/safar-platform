package com.safar.user.controller;

import com.safar.user.entity.LoyaltyTransaction;
import com.safar.user.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(loyaltyService.getLoyaltyStatus(userId));
    }

    @GetMapping("/discount")
    public ResponseEntity<Map<String, Integer>> getDiscount(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(Map.of("discountPercent", loyaltyService.getDiscountPercent(userId)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<LoyaltyTransaction>> getTransactions(
            @RequestHeader("X-User-Id") UUID userId, Pageable pageable) {
        return ResponseEntity.ok(loyaltyService.getTransactions(userId, pageable));
    }

    @PostMapping("/redeem")
    public ResponseEntity<Void> redeemPoints(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam long points,
            @RequestParam String description) {
        loyaltyService.redeemPoints(userId, points, description);
        return ResponseEntity.ok().build();
    }
}
