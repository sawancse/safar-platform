package com.safar.listing.repository;

import com.safar.listing.entity.ListingDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingDraftRepository extends JpaRepository<ListingDraft, UUID> {
    List<ListingDraft> findByHostId(UUID hostId);
    Optional<ListingDraft> findById(UUID id);
}
