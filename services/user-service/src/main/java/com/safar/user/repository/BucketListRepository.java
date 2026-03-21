package com.safar.user.repository;

import com.safar.user.entity.BucketListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BucketListRepository extends JpaRepository<BucketListItem, UUID> {

    Page<BucketListItem> findByGuestId(UUID guestId, Pageable pageable);

    boolean existsByGuestIdAndListingId(UUID guestId, UUID listingId);

    void deleteByGuestIdAndListingId(UUID guestId, UUID listingId);
}
