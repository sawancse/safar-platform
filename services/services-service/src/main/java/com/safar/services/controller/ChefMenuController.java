package com.safar.services.controller;

import com.safar.services.dto.*;
import com.safar.services.entity.ChefMenu;
import com.safar.services.entity.MenuItem;
import com.safar.services.service.ChefMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chefs")
@RequiredArgsConstructor
public class ChefMenuController {

    private final ChefMenuService chefMenuService;

    @PostMapping("/{chefId}/menus")
    public ResponseEntity<ChefMenu> createMenu(Authentication auth,
                                                @PathVariable UUID chefId,
                                                @RequestBody CreateChefMenuRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chefMenuService.createMenu(userId, chefId, req));
    }

    @GetMapping("/{chefId}/menus")
    public ResponseEntity<List<ChefMenu>> getMenus(@PathVariable UUID chefId) {
        return ResponseEntity.ok(chefMenuService.getMenus(chefId));
    }

    @GetMapping("/menus/{menuId}/items")
    public ResponseEntity<List<MenuItem>> getMenuItems(@PathVariable UUID menuId) {
        return ResponseEntity.ok(chefMenuService.getMenuItems(menuId));
    }

    @DeleteMapping("/menus/{menuId}")
    public ResponseEntity<Void> deleteMenu(Authentication auth,
                                            @PathVariable UUID menuId) {
        UUID userId = UUID.fromString(auth.getName());
        chefMenuService.deleteMenu(userId, menuId);
        return ResponseEntity.noContent().build();
    }

    // ── Ingredients ─────────────────────────────────────────

    @GetMapping("/menu-items/{menuItemId}/ingredients")
    public ResponseEntity<List<IngredientResponse>> getIngredients(@PathVariable UUID menuItemId) {
        return ResponseEntity.ok(chefMenuService.getIngredients(menuItemId));
    }

    @PutMapping("/menu-items/{menuItemId}/ingredients")
    public ResponseEntity<List<IngredientResponse>> setIngredients(
            @PathVariable UUID menuItemId,
            @RequestBody List<IngredientRequest> ingredients) {
        return ResponseEntity.ok(chefMenuService.setIngredients(menuItemId, ingredients));
    }

    // ── Shopping List ───────────────────────────────────────

    @GetMapping("/menus/{menuId}/shopping-list")
    public ResponseEntity<ShoppingListResponse> getShoppingList(
            @PathVariable UUID menuId,
            @RequestParam(defaultValue = "4") int guests) {
        return ResponseEntity.ok(chefMenuService.generateShoppingList(menuId, guests));
    }
}
