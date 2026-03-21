package com.safar.listing.repository;

import com.safar.listing.entity.ICalFeed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ICalFeedRepository extends JpaRepository<ICalFeed, UUID> {
    List<ICalFeed> findByListingId(UUID listingId);
    List<ICalFeed> findByIsActiveTrue();
    List<ICalFeed> findByIsActiveTrueAndLastSyncedAtBeforeOrLastSyncedAtIsNull(OffsetDateTime before);
}
