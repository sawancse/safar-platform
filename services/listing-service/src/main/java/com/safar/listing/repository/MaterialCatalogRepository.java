package com.safar.listing.repository;

import com.safar.listing.entity.MaterialCatalog;
import com.safar.listing.entity.enums.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaterialCatalogRepository extends JpaRepository<MaterialCatalog, UUID> {

    List<MaterialCatalog> findByActiveTrue();

    List<MaterialCatalog> findByCategoryAndActiveTrue(MaterialCategory category);
}
