package com.safar.listing.repository;

import com.safar.listing.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    List<RoomType> findByListingIdOrderBySortOrder(UUID listingId);

    void deleteByListingIdAndId(UUID listingId, UUID id);
}
