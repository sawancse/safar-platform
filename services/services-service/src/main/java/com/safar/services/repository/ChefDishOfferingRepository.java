package com.safar.services.repository;

import com.safar.services.entity.ChefDishOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChefDishOfferingRepository extends JpaRepository<ChefDishOffering, UUID> {

    List<ChefDishOffering> findByChefId(UUID chefId);

    List<ChefDishOffering> findByDishIdIn(List<UUID> dishIds);

    Optional<ChefDishOffering> findByChefIdAndDishId(UUID chefId, UUID dishId);

    void deleteByChefIdAndDishId(UUID chefId, UUID dishId);

    long countByChefId(UUID chefId);
}
