package com.safar.chef.service;

import com.safar.chef.dto.*;
import com.safar.chef.entity.ChefMenu;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.DishIngredient;
import com.safar.chef.entity.MenuItem;
import com.safar.chef.entity.enums.CuisineType;
import com.safar.chef.entity.enums.MealType;
import com.safar.chef.entity.enums.ServiceType;
import com.safar.chef.repository.ChefMenuRepository;
import com.safar.chef.repository.ChefProfileRepository;
import com.safar.chef.repository.DishIngredientRepository;
import com.safar.chef.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChefMenuService {

    private final ChefMenuRepository menuRepo;
    private final MenuItemRepository menuItemRepo;
    private final ChefProfileRepository chefProfileRepo;
    private final DishIngredientRepository ingredientRepo;

    @Transactional
    public ChefMenu createMenu(UUID userId, UUID chefId, CreateChefMenuRequest req) {
        ChefProfile chef = chefProfileRepo.findById(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));

        if (!chef.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to create menu for this chef");
        }

        ChefMenu menu = ChefMenu.builder()
                .chefId(chefId)
                .name(req.name())
                .description(req.description())
                .serviceType(req.serviceType() != null ? ServiceType.valueOf(req.serviceType()) : ServiceType.DAILY)
                .cuisineType(req.cuisineType() != null ? CuisineType.valueOf(req.cuisineType()) : null)
                .mealType(req.mealType() != null ? MealType.valueOf(req.mealType()) : null)
                .pricePerPlatePaise(req.pricePerPlatePaise())
                .minGuests(req.minGuests() != null ? req.minGuests() : 1)
                .maxGuests(req.maxGuests())
                .isVeg(Boolean.TRUE.equals(req.isVeg()))
                .isVegan(Boolean.TRUE.equals(req.isVegan()))
                .isJain(Boolean.TRUE.equals(req.isJain()))
                .active(true)
                .build();

        ChefMenu saved = menuRepo.save(menu);

        // Save menu items + ingredients
        if (req.items() != null && !req.items().isEmpty()) {
            int sortOrder = 0;
            for (MenuItemRequest itemReq : req.items()) {
                MenuItem item = MenuItem.builder()
                        .menuId(saved.getId())
                        .name(itemReq.name())
                        .description(itemReq.description())
                        .category(itemReq.category())
                        .isVeg(itemReq.isVeg() != null ? itemReq.isVeg() : true)
                        .sortOrder(sortOrder++)
                        .build();
                item = menuItemRepo.save(item);

                // Save ingredients for this dish
                if (itemReq.ingredients() != null) {
                    saveIngredients(item.getId(), itemReq.ingredients());
                }
            }
        }

        log.info("Chef menu created: {} for chef={}", saved.getId(), chefId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ChefMenu> getMenus(UUID chefId) {
        return menuRepo.findByChefIdAndActiveTrue(chefId);
    }

    @Transactional(readOnly = true)
    public List<MenuItem> getMenuItems(UUID menuId) {
        return menuItemRepo.findByMenuIdOrderBySortOrder(menuId);
    }

    @Transactional
    public void deleteMenu(UUID userId, UUID menuId) {
        ChefMenu menu = menuRepo.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found"));

        ChefProfile chef = chefProfileRepo.findById(menu.getChefId())
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));

        if (!chef.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this menu");
        }

        menu.setActive(false);
        menuRepo.save(menu);
        log.info("Chef menu soft-deleted: {}", menuId);
    }

    // ── Ingredients ─────────────────────────────────────────

    @Transactional
    public List<IngredientResponse> setIngredients(UUID menuItemId, List<IngredientRequest> ingredients) {
        menuItemRepo.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + menuItemId));
        ingredientRepo.deleteByMenuItemId(menuItemId);
        saveIngredients(menuItemId, ingredients);
        return getIngredients(menuItemId);
    }

    public List<IngredientResponse> getIngredients(UUID menuItemId) {
        return ingredientRepo.findByMenuItemIdOrderBySortOrder(menuItemId).stream()
                .map(i -> new IngredientResponse(i.getId(), i.getMenuItemId(), i.getName(),
                        i.getQuantity(), i.getUnit(), i.getCategory(),
                        i.getIsOptional(), i.getNotes()))
                .toList();
    }

    // ── Shopping List Generator ──────────────────────────────

    public ShoppingListResponse generateShoppingList(UUID menuId, int guestCount) {
        ChefMenu menu = menuRepo.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + menuId));

        List<MenuItem> items = menuItemRepo.findByMenuIdOrderBySortOrder(menuId);
        List<UUID> itemIds = items.stream().map(MenuItem::getId).toList();
        List<DishIngredient> allIngredients = ingredientRepo.findByMenuItemIdInOrderByCategoryAscNameAsc(itemIds);

        if (allIngredients.isEmpty()) {
            return new ShoppingListResponse(menuId, menu.getName(), guestCount, List.of());
        }

        // Build dish name lookup
        Map<UUID, String> dishNames = items.stream()
                .collect(Collectors.toMap(MenuItem::getId, MenuItem::getName));

        // Aggregate: same ingredient name + unit → sum quantities across dishes
        record AggKey(String name, String unit, String category) {}
        Map<AggKey, BigDecimal> aggregated = new LinkedHashMap<>();
        Map<AggKey, Boolean> optionalMap = new HashMap<>();
        Map<AggKey, String> notesMap = new HashMap<>();
        Map<AggKey, List<String>> fromDishes = new HashMap<>();

        BigDecimal guests = BigDecimal.valueOf(Math.max(1, guestCount));

        for (DishIngredient ing : allIngredients) {
            AggKey key = new AggKey(ing.getName().toLowerCase().trim(), ing.getUnit(), ing.getCategory());
            BigDecimal scaled = ing.getQuantity() != null
                    ? ing.getQuantity().multiply(guests).setScale(1, RoundingMode.CEILING)
                    : null;
            aggregated.merge(key, scaled != null ? scaled : BigDecimal.ZERO, BigDecimal::add);
            optionalMap.merge(key, Boolean.TRUE.equals(ing.getIsOptional()), (a, b) -> a && b);
            if (ing.getNotes() != null) notesMap.putIfAbsent(key, ing.getNotes());
            fromDishes.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(dishNames.getOrDefault(ing.getMenuItemId(), ""));
        }

        // Group by category
        Map<String, List<ShoppingListResponse.ShoppingItem>> categoryItems = new LinkedHashMap<>();
        for (var entry : aggregated.entrySet()) {
            AggKey key = entry.getKey();
            BigDecimal total = entry.getValue();
            BigDecimal perServing = guestCount > 0 && total.compareTo(BigDecimal.ZERO) > 0
                    ? total.divide(guests, 2, RoundingMode.HALF_UP) : null;

            ShoppingListResponse.ShoppingItem item = new ShoppingListResponse.ShoppingItem(
                    key.name().substring(0, 1).toUpperCase() + key.name().substring(1),
                    perServing, total, key.unit(),
                    optionalMap.getOrDefault(key, false),
                    notesMap.get(key),
                    String.join(", ", fromDishes.getOrDefault(key, List.of()))
            );
            categoryItems.computeIfAbsent(key.category(), k -> new ArrayList<>()).add(item);
        }

        List<ShoppingListResponse.ShoppingCategory> categories = categoryItems.entrySet().stream()
                .map(e -> new ShoppingListResponse.ShoppingCategory(e.getKey(), e.getValue()))
                .toList();

        return new ShoppingListResponse(menuId, menu.getName(), guestCount, categories);
    }

    // ── Private Helpers ──────────────────────────────────────

    private void saveIngredients(UUID menuItemId, List<IngredientRequest> ingredients) {
        int order = 0;
        for (IngredientRequest req : ingredients) {
            DishIngredient ing = DishIngredient.builder()
                    .menuItemId(menuItemId)
                    .name(req.name())
                    .quantity(req.quantity())
                    .unit(req.unit())
                    .category(req.category() != null ? req.category() : "GROCERY")
                    .isOptional(Boolean.TRUE.equals(req.isOptional()))
                    .notes(req.notes())
                    .sortOrder(order++)
                    .build();
            ingredientRepo.save(ing);
        }
    }
}
