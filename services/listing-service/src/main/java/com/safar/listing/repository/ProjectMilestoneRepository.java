package com.safar.listing.repository;

import com.safar.listing.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID> {

    List<ProjectMilestone> findByInteriorProjectIdOrderByPlannedDateAsc(UUID interiorProjectId);
}
