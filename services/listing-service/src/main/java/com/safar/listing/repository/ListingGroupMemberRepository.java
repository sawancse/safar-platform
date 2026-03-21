package com.safar.listing.repository;

import com.safar.listing.entity.ListingGroupMember;
import com.safar.listing.entity.ListingGroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingGroupMemberRepository extends JpaRepository<ListingGroupMember, ListingGroupMemberId> {
    List<ListingGroupMember> findByGroupId(UUID groupId);
    void deleteByGroupIdAndListingId(UUID groupId, UUID listingId);
}
