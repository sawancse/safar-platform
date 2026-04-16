package com.safar.chef.controller;

import com.safar.chef.dto.*;
import com.safar.chef.entity.DishCatalog;
import com.safar.chef.entity.enums.DishCategory;
import com.safar.chef.service.DishCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DishCatalogController {

    private final DishCatalogService dishCatalogService;

    // ── Public: browse dish catalog ──────────────────────────────────────

    @GetMapping("/api/v1/dishes")
    public ResponseEntity<?> getDishes(
            @RequestParam(required = false) DishCategory category) {
        if (category != null) {
            return ResponseEntity.ok(dishCatalogService.getDishesByCategory(category));
        }
        return ResponseEntity.ok(dishCatalogService.getAllDishes());
    }

    // ── Public: match chefs for selected dishes ─────────────────────────

    @PostMapping("/api/v1/dishes/match-chefs")
    public ResponseEntity<List<MatchedChefResponse>> matchChefs(
            @RequestBody MatchChefsRequest request) {
        return ResponseEntity.ok(dishCatalogService.findMatchingChefs(request.dishIds()));
    }

    // ── Chef: manage dish offerings ─────────────────────────────────────

    @PostMapping("/api/v1/chefs/{chefId}/dish-offerings")
    public ResponseEntity<List<ChefDishOfferingResponse>> addDishOfferings(
            @PathVariable UUID chefId,
            @RequestBody AddDishOfferingsRequest request,
            Authentication auth) {
        // Auth check: only the chef themselves can add offerings
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dishCatalogService.addDishOfferings(chefId, request));
    }

    @GetMapping("/api/v1/chefs/{chefId}/dish-offerings")
    public ResponseEntity<List<ChefDishOfferingResponse>> getChefDishOfferings(
            @PathVariable UUID chefId) {
        return ResponseEntity.ok(dishCatalogService.getChefDishOfferings(chefId));
    }

    @DeleteMapping("/api/v1/chefs/{chefId}/dish-offerings/{dishId}")
    public ResponseEntity<Void> removeDishOffering(
            @PathVariable UUID chefId,
            @PathVariable UUID dishId,
            Authentication auth) {
        dishCatalogService.removeDishOffering(chefId, dishId);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        }
    }

    // ── Admin: dish catalog CRUD ────────────────────────────────────────

    @GetMapping("/api/v1/dishes/admin/all")
    public ResponseEntity<List<DishCatalog>> adminGetAllDishes(Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(dishCatalogService.adminGetAllDishes());
    }

    @PostMapping("/api/v1/dishes/admin")
    public ResponseEntity<DishCatalog> adminCreateDish(@RequestBody DishCatalog dish, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dishCatalogService.adminCreateDish(dish));
    }

    @PutMapping("/api/v1/dishes/admin/{dishId}")
    public ResponseEntity<DishCatalog> adminUpdateDish(@PathVariable UUID dishId,
                                                        @RequestBody DishCatalog dish,
                                                        Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(dishCatalogService.adminUpdateDish(dishId, dish));
    }

    @DeleteMapping("/api/v1/dishes/admin/{dishId}")
    public ResponseEntity<Void> adminDeleteDish(@PathVariable UUID dishId, Authentication auth) {
        requireAdmin(auth);
        dishCatalogService.adminDeleteDish(dishId);
        return ResponseEntity.noContent().build();
    }
}
