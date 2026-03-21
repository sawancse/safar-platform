package com.safar.listing.repository;

import com.safar.listing.entity.RoomTypeInclusion;
import com.safar.listing.entity.enums.InclusionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomTypeInclusionRepository extends JpaRepository<RoomTypeInclusion, UUID> {

    List<RoomTypeInclusion> findByRoomTypeIdAndIsActiveTrueOrderBySortOrder(UUID roomTypeId);

    List<RoomTypeInclusion> findByRoomTypeIdOrderBySortOrder(UUID roomTypeId);

    List<RoomTypeInclusion> findByRoomTypeIdAndCategoryAndIsActiveTrue(UUID roomTypeId, InclusionCategory category);

    void deleteByRoomTypeId(UUID roomTypeId);
}
