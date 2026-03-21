package com.safar.listing.repository;

import com.safar.listing.entity.ListingGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingGroupRepository extends JpaRepository<ListingGroup, UUID> {
    List<ListingGroup> findByHostId(UUID hostId);
}
