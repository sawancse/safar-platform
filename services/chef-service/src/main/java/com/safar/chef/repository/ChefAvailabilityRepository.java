package com.safar.chef.repository;

import com.safar.chef.entity.ChefAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChefAvailabilityRepository extends JpaRepository<ChefAvailability, UUID> {
    List<ChefAvailability> findByChefIdAndBlockedDateBetween(UUID chefId, LocalDate from, LocalDate to);
    List<ChefAvailability> findByChefId(UUID chefId);
    Optional<ChefAvailability> findByChefIdAndBlockedDate(UUID chefId, LocalDate date);
    void deleteByChefIdAndBlockedDate(UUID chefId, LocalDate date);
    boolean existsByChefIdAndBlockedDate(UUID chefId, LocalDate date);
}
