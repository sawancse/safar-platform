package com.safar.services.repository;

import com.safar.services.entity.ChefPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChefPhotoRepository extends JpaRepository<ChefPhoto, UUID> {
    List<ChefPhoto> findByChefIdOrderBySortOrder(UUID chefId);
    int countByChefId(UUID chefId);
}
