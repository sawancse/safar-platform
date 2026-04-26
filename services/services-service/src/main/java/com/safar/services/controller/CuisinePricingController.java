package com.safar.services.controller;

import com.safar.services.entity.CuisinePriceTier;
import com.safar.services.service.CuisinePricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chefs/cuisine-pricing")
@RequiredArgsConstructor
public class CuisinePricingController {

    private final CuisinePricingService pricingService;

    @PutMapping("/{cuisineType}")
    public ResponseEntity<CuisinePriceTier> setPricing(Authentication auth,
                                                        @PathVariable String cuisineType,
                                                        @RequestParam Long pricePerPlatePaise) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(pricingService.setPricing(userId, cuisineType, pricePerPlatePaise));
    }

    @GetMapping("/{chefId}")
    public ResponseEntity<List<CuisinePriceTier>> getChefPricing(@PathVariable UUID chefId) {
        return ResponseEntity.ok(pricingService.getChefPricing(chefId));
    }

    @DeleteMapping("/{cuisineType}")
    public ResponseEntity<Void> deletePricing(Authentication auth, @PathVariable String cuisineType) {
        UUID userId = UUID.fromString(auth.getName());
        pricingService.deletePricing(userId, cuisineType);
        return ResponseEntity.noContent().build();
    }
}
