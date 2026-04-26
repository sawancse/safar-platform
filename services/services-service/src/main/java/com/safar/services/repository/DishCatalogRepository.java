package com.safar.services.repository;

import com.safar.services.entity.DishCatalog;
import com.safar.services.entity.enums.DishCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DishCatalogRepository extends JpaRepository<DishCatalog, UUID> {

    List<DishCatalog> findByCategoryAndActiveTrueOrderBySortOrder(DishCategory category);

    List<DishCatalog> findByActiveTrueOrderByCategoryAscSortOrderAsc();
}
