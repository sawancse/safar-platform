package com.safar.chef.service;

import com.safar.chef.dto.CreateChefMenuRequest;
import com.safar.chef.dto.MenuItemRequest;
import com.safar.chef.entity.ChefMenu;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.MenuItem;
import com.safar.chef.entity.enums.CuisineType;
import com.safar.chef.entity.enums.MealType;
import com.safar.chef.entity.enums.ServiceType;
import com.safar.chef.repository.ChefMenuRepository;
import com.safar.chef.repository.ChefProfileRepository;
import com.safar.chef.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChefMenuService {

    private final ChefMenuRepository menuRepo;
    private final MenuItemRepository menuItemRepo;
    private final ChefProfileRepository chefProfileRepo;

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

        // Save menu items
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
                menuItemRepo.save(item);
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
}
