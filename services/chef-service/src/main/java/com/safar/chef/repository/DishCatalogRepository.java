package com.safar.chef.repository;

import com.safar.chef.entity.DishCatalog;
import com.safar.chef.entity.enums.DishCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DishCatalogRepository extends JpaRepository<DishCatalog, UUID> {

    List<DishCatalog> findByCategoryAndActiveTrueOrderBySortOrder(DishCategory category);

    List<DishCatalog> findByActiveTrueOrderByCategoryAscSortOrderAsc();
}
