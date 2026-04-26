package com.safar.services.service;

import com.safar.services.dto.*;
import com.safar.services.entity.ChefDishOffering;
import com.safar.services.entity.ChefProfile;
import com.safar.services.entity.DishCatalog;
import com.safar.services.entity.enums.DishCategory;
import com.safar.services.repository.ChefDishOfferingRepository;
import com.safar.services.repository.ChefProfileRepository;
import com.safar.services.repository.DishCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishCatalogService {

    private final DishCatalogRepository dishCatalogRepo;
    private final ChefDishOfferingRepository chefDishOfferingRepo;
    private final ChefProfileRepository chefProfileRepo;

    /**
     * Returns all active dishes grouped by category.
     */
    public Map<DishCategory, List<DishCatalogResponse>> getAllDishes() {
        return dishCatalogRepo.findByActiveTrueOrderByCategoryAscSortOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.groupingBy(DishCatalogResponse::category, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Returns active dishes filtered by category.
     */
    public List<DishCatalogResponse> getDishesByCategory(DishCategory category) {
        return dishCatalogRepo.findByCategoryAndActiveTrueOrderBySortOrder(category)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Find chefs who have offerings for the selected dishes, ranked by how many
     * dishes they can cook (descending). Returns MatchedChefResponse list.
     */
    public List<MatchedChefResponse> findMatchingChefs(List<UUID> dishIds) {
        if (dishIds == null || dishIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Load catalog dishes for price lookup
        Map<UUID, DishCatalog> dishMap = dishCatalogRepo.findAllById(dishIds)
                .stream()
                .collect(Collectors.toMap(DishCatalog::getId, d -> d));

        // Find all offerings for the requested dishes
        List<ChefDishOffering> offerings = chefDishOfferingRepo.findByDishIdIn(dishIds);

        // Group by chefId
        Map<UUID, List<ChefDishOffering>> byChef = offerings.stream()
                .collect(Collectors.groupingBy(ChefDishOffering::getChefId));

        // Build result for each chef
        List<MatchedChefResponse> results = new ArrayList<>();
        for (Map.Entry<UUID, List<ChefDishOffering>> entry : byChef.entrySet()) {
            UUID chefId = entry.getKey();
            List<ChefDishOffering> chefOfferings = entry.getValue();

            ChefProfile chef = chefProfileRepo.findById(chefId).orElse(null);
            if (chef == null || !Boolean.TRUE.equals(chef.getAvailable())) continue;

            // Calculate estimated price (sum of custom or catalog price for matched dishes)
            long estimatedPrice = 0;
            for (ChefDishOffering o : chefOfferings) {
                DishCatalog dish = dishMap.get(o.getDishId());
                if (dish != null) {
                    estimatedPrice += (o.getCustomPricePaise() != null)
                            ? o.getCustomPricePaise()
                            : dish.getPricePaise();
                }
            }

            long totalDishCount = chefDishOfferingRepo.countByChefId(chefId);

            results.add(new MatchedChefResponse(
                    chefId,
                    chef.getName(),
                    chef.getProfilePhotoUrl(),
                    chef.getCity(),
                    chef.getRating(),
                    chef.getReviewCount(),
                    chef.getTotalBookings(),
                    chef.getExperienceYears(),
                    chefOfferings.size(),
                    totalDishCount,
                    estimatedPrice,
                    chef.getCuisines(),
                    chef.getVerified(),
                    chef.getBadge()
            ));
        }

        // Sort by matched dish count desc, then rating desc
        results.sort(Comparator
                .comparingInt(MatchedChefResponse::matchedDishCount).reversed()
                .thenComparing(Comparator.comparingDouble(MatchedChefResponse::rating).reversed()));

        return results;
    }

    /**
     * Chef adds dishes they can cook.
     */
    @Transactional
    public List<ChefDishOfferingResponse> addDishOfferings(UUID chefId, AddDishOfferingsRequest req) {
        chefProfileRepo.findById(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef not found: " + chefId));

        List<ChefDishOffering> created = new ArrayList<>();
        for (UUID dishId : req.dishIds()) {
            // Skip if already exists
            if (chefDishOfferingRepo.findByChefIdAndDishId(chefId, dishId).isPresent()) {
                continue;
            }
            dishCatalogRepo.findById(dishId)
                    .orElseThrow(() -> new IllegalArgumentException("Dish not found: " + dishId));

            ChefDishOffering offering = ChefDishOffering.builder()
                    .chefId(chefId)
                    .dishId(dishId)
                    .customPricePaise(req.customPricePaise())
                    .build();
            created.add(chefDishOfferingRepo.save(offering));
        }
        log.info("Chef {} added {} dish offerings", chefId, created.size());
        return getChefDishOfferings(chefId);
    }

    /**
     * Get a chef's dish offerings with catalog details.
     */
    public List<ChefDishOfferingResponse> getChefDishOfferings(UUID chefId) {
        List<ChefDishOffering> offerings = chefDishOfferingRepo.findByChefId(chefId);
        if (offerings.isEmpty()) return Collections.emptyList();

        List<UUID> dishIds = offerings.stream().map(ChefDishOffering::getDishId).toList();
        Map<UUID, DishCatalog> dishMap = dishCatalogRepo.findAllById(dishIds)
                .stream()
                .collect(Collectors.toMap(DishCatalog::getId, d -> d));

        return offerings.stream()
                .map(o -> {
                    DishCatalog dish = dishMap.get(o.getDishId());
                    if (dish == null) return null;
                    long effective = (o.getCustomPricePaise() != null)
                            ? o.getCustomPricePaise()
                            : dish.getPricePaise();
                    return new ChefDishOfferingResponse(
                            dish.getId(),
                            dish.getName(),
                            dish.getCategory(),
                            dish.getPricePaise(),
                            o.getCustomPricePaise(),
                            effective,
                            dish.getIsVeg(),
                            dish.getPhotoUrl()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Remove a dish offering from a chef.
     */
    @Transactional
    public void removeDishOffering(UUID chefId, UUID dishId) {
        chefDishOfferingRepo.deleteByChefIdAndDishId(chefId, dishId);
        log.info("Chef {} removed dish offering {}", chefId, dishId);
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────

    public List<DishCatalog> adminGetAllDishes() {
        return dishCatalogRepo.findAll(org.springframework.data.domain.Sort.by("category", "sortOrder"));
    }

    @Transactional
    public DishCatalog adminCreateDish(DishCatalog dish) {
        DishCatalog saved = dishCatalogRepo.save(dish);
        log.info("Admin created dish: {} ({})", saved.getName(), saved.getCategory());
        return saved;
    }

    @Transactional
    public DishCatalog adminUpdateDish(UUID dishId, DishCatalog updates) {
        DishCatalog dish = dishCatalogRepo.findById(dishId)
                .orElseThrow(() -> new IllegalArgumentException("Dish not found: " + dishId));
        if (updates.getName() != null) dish.setName(updates.getName());
        if (updates.getDescription() != null) dish.setDescription(updates.getDescription());
        if (updates.getCategory() != null) dish.setCategory(updates.getCategory());
        if (updates.getPricePaise() != null) dish.setPricePaise(updates.getPricePaise());
        if (updates.getPhotoUrl() != null) dish.setPhotoUrl(updates.getPhotoUrl());
        if (updates.getIsVeg() != null) dish.setIsVeg(updates.getIsVeg());
        if (updates.getIsRecommended() != null) dish.setIsRecommended(updates.getIsRecommended());
        if (updates.getNoOnionGarlic() != null) dish.setNoOnionGarlic(updates.getNoOnionGarlic());
        if (updates.getIsFried() != null) dish.setIsFried(updates.getIsFried());
        if (updates.getSortOrder() != null) dish.setSortOrder(updates.getSortOrder());
        if (updates.getActive() != null) dish.setActive(updates.getActive());
        log.info("Admin updated dish: {} ({})", dish.getName(), dish.getId());
        return dishCatalogRepo.save(dish);
    }

    @Transactional
    public void adminDeleteDish(UUID dishId) {
        DishCatalog dish = dishCatalogRepo.findById(dishId)
                .orElseThrow(() -> new IllegalArgumentException("Dish not found: " + dishId));
        dish.setActive(false);
        dishCatalogRepo.save(dish);
        log.info("Admin deactivated dish: {} ({})", dish.getName(), dishId);
    }

    private DishCatalogResponse toResponse(DishCatalog d) {
        return new DishCatalogResponse(
                d.getId(),
                d.getName(),
                d.getDescription(),
                d.getCategory(),
                d.getPricePaise(),
                d.getPhotoUrl(),
                d.getIsVeg(),
                d.getIsRecommended(),
                d.getNoOnionGarlic(),
                d.getIsFried()
        );
    }
}
