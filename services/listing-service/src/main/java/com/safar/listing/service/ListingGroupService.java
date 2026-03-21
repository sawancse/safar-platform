package com.safar.listing.service;

import com.safar.listing.dto.CreateGroupRequest;
import com.safar.listing.dto.ListingGroupResponse;
import com.safar.listing.entity.ListingGroup;
import com.safar.listing.entity.ListingGroupMember;
import com.safar.listing.repository.ListingGroupMemberRepository;
import com.safar.listing.repository.ListingGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingGroupService {

    private final ListingGroupRepository groupRepository;
    private final ListingGroupMemberRepository memberRepository;

    @Transactional
    public ListingGroupResponse createGroup(UUID hostId, CreateGroupRequest req) {
        ListingGroup group = ListingGroup.builder()
                .hostId(hostId)
                .name(req.name())
                .bundleDiscountPct(req.bundleDiscountPct() != null ? req.bundleDiscountPct() : 0)
                .build();

        ListingGroup saved = groupRepository.save(group);

        List<ListingGroupMember> members = req.listingIds().stream()
                .map(listingId -> ListingGroupMember.builder()
                        .groupId(saved.getId())
                        .listingId(listingId)
                        .build())
                .toList();
        memberRepository.saveAll(members);

        log.info("Created listing group {} with {} members for host {}",
                saved.getId(), members.size(), hostId);

        return toResponse(saved, req.listingIds());
    }

    public ListingGroupResponse getGroup(UUID groupId) {
        ListingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Listing group not found: " + groupId));

        List<UUID> listingIds = memberRepository.findByGroupId(groupId).stream()
                .map(ListingGroupMember::getListingId)
                .toList();

        return toResponse(group, listingIds);
    }

    @Transactional
    public ListingGroupResponse addMember(UUID groupId, UUID listingId) {
        ListingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Listing group not found: " + groupId));

        ListingGroupMember member = ListingGroupMember.builder()
                .groupId(groupId)
                .listingId(listingId)
                .build();
        memberRepository.save(member);

        log.info("Added listing {} to group {}", listingId, groupId);

        List<UUID> listingIds = memberRepository.findByGroupId(groupId).stream()
                .map(ListingGroupMember::getListingId)
                .toList();

        return toResponse(group, listingIds);
    }

    @Transactional
    public ListingGroupResponse removeMember(UUID groupId, UUID listingId) {
        ListingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Listing group not found: " + groupId));

        memberRepository.deleteByGroupIdAndListingId(groupId, listingId);

        log.info("Removed listing {} from group {}", listingId, groupId);

        List<UUID> listingIds = memberRepository.findByGroupId(groupId).stream()
                .map(ListingGroupMember::getListingId)
                .toList();

        return toResponse(group, listingIds);
    }

    private ListingGroupResponse toResponse(ListingGroup group, List<UUID> listingIds) {
        return new ListingGroupResponse(
                group.getId(),
                group.getHostId(),
                group.getName(),
                group.getBundleDiscountPct(),
                listingIds,
                group.getCreatedAt()
        );
    }
}
