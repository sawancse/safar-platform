package com.safar.services.repository;

import com.safar.services.entity.CuisinePriceTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuisinePriceTierRepository extends JpaRepository<CuisinePriceTier, UUID> {
    List<CuisinePriceTier> findByChefId(UUID chefId);
    Optional<CuisinePriceTier> findByChefIdAndCuisineType(UUID chefId, String cuisineType);
}
