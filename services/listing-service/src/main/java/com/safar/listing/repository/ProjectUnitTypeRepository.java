package com.safar.listing.repository;

import com.safar.listing.entity.ProjectUnitType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectUnitTypeRepository extends JpaRepository<ProjectUnitType, UUID> {

    List<ProjectUnitType> findByProjectIdOrderByBhkAscBasePricePaiseAsc(UUID projectId);

    List<ProjectUnitType> findByProjectIdAndBhk(UUID projectId, Integer bhk);
}
