package com.safar.listing.repository;

import com.safar.listing.entity.RoomDesign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomDesignRepository extends JpaRepository<RoomDesign, UUID> {

    List<RoomDesign> findByInteriorProjectId(UUID interiorProjectId);
}
