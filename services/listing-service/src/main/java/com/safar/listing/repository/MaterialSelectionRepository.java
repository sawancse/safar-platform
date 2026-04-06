package com.safar.listing.repository;

import com.safar.listing.entity.MaterialSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaterialSelectionRepository extends JpaRepository<MaterialSelection, UUID> {

    List<MaterialSelection> findByProjectId(UUID projectId);
}
