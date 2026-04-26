package com.safar.services.repository;

import com.safar.services.entity.ChefMenu;
import com.safar.services.entity.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChefMenuRepository extends JpaRepository<ChefMenu, UUID> {

    List<ChefMenu> findByChefId(UUID chefId);

    List<ChefMenu> findByChefIdAndActiveTrue(UUID chefId);

    List<ChefMenu> findByChefIdAndServiceType(UUID chefId, ServiceType serviceType);
}
