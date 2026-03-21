package com.safar.listing.repository;

import com.safar.listing.entity.ListingMedia;
import com.safar.listing.entity.enums.MediaType;
import com.safar.listing.entity.enums.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingMediaRepository extends JpaRepository<ListingMedia, UUID> {

    List<ListingMedia> findByListingId(UUID listingId);

    List<ListingMedia> findByListingIdOrderBySortOrderAsc(UUID listingId);

    boolean existsByListingIdAndTypeAndModerationStatus(
            UUID listingId, MediaType type, ModerationStatus moderationStatus);

    boolean existsByListingIdAndCdnUrl(UUID listingId, String cdnUrl);

    long countByListingIdAndTypeAndModerationStatus(
            UUID listingId, MediaType type, ModerationStatus moderationStatus);

    boolean existsByListingIdAndIsPrimaryTrue(UUID listingId);

    ListingMedia findFirstByListingIdAndIsPrimaryTrue(UUID listingId);
}
