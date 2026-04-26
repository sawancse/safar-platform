package com.safar.services.repository;

import com.safar.services.entity.ChefEventPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChefEventPricingRepository extends JpaRepository<ChefEventPricing, UUID> {

    List<ChefEventPricing> findByChefId(UUID chefId);

    Optional<ChefEventPricing> findByChefIdAndItemKey(UUID chefId, String itemKey);

    void deleteByChefIdAndItemKey(UUID chefId, String itemKey);
}
