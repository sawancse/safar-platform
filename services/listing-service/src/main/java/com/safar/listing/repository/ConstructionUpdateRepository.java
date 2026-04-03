package com.safar.listing.repository;

import com.safar.listing.entity.ConstructionUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConstructionUpdateRepository extends JpaRepository<ConstructionUpdate, UUID> {

    List<ConstructionUpdate> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
