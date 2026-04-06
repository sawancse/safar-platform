package com.safar.listing.repository;

import com.safar.listing.entity.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QualityCheckRepository extends JpaRepository<QualityCheck, UUID> {

    List<QualityCheck> findByInteriorProjectId(UUID interiorProjectId);

    List<QualityCheck> findByMilestoneId(UUID milestoneId);
}
