package com.safar.chef.repository;

import com.safar.chef.entity.DishIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DishIngredientRepository extends JpaRepository<DishIngredient, UUID> {

    List<DishIngredient> findByMenuItemIdOrderBySortOrder(UUID menuItemId);

    List<DishIngredient> findByMenuItemIdInOrderByCategoryAscNameAsc(List<UUID> menuItemIds);

    void deleteByMenuItemId(UUID menuItemId);
}
