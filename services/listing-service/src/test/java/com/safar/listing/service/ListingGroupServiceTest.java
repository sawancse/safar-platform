package com.safar.listing.service;

import com.safar.listing.dto.CreateGroupRequest;
import com.safar.listing.dto.ListingGroupResponse;
import com.safar.listing.entity.ListingGroup;
import com.safar.listing.entity.ListingGroupMember;
import com.safar.listing.repository.ListingGroupMemberRepository;
import com.safar.listing.repository.ListingGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingGroupServiceTest {

    @Mock
    ListingGroupRepository groupRepository;

    @Mock
    ListingGroupMemberRepository memberRepository;

    @InjectMocks
    ListingGroupService listingGroupService;

    private final UUID hostId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final UUID listingId1 = UUID.randomUUID();
    private final UUID listingId2 = UUID.randomUUID();

    @Test
    void createGroup_withMembers_savesGroupAndMembers() {
        CreateGroupRequest req = new CreateGroupRequest("Beach Villas", 10, List.of(listingId1, listingId2));

        ListingGroup savedGroup = ListingGroup.builder()
                .id(groupId)
                .hostId(hostId)
                .name("Beach Villas")
                .bundleDiscountPct(10)
                .createdAt(OffsetDateTime.now())
                .build();

        when(groupRepository.save(any(ListingGroup.class))).thenReturn(savedGroup);
        when(memberRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ListingGroupResponse response = listingGroupService.createGroup(hostId, req);

        assertThat(response.id()).isEqualTo(groupId);
        assertThat(response.hostId()).isEqualTo(hostId);
        assertThat(response.name()).isEqualTo("Beach Villas");
        assertThat(response.bundleDiscountPct()).isEqualTo(10);
        assertThat(response.listingIds()).containsExactly(listingId1, listingId2);
        verify(groupRepository).save(any(ListingGroup.class));
        verify(memberRepository).saveAll(anyList());
    }

    @Test
    void getGroup_returnsCorrectMembers() {
        ListingGroup group = ListingGroup.builder()
                .id(groupId)
                .hostId(hostId)
                .name("Beach Villas")
                .bundleDiscountPct(10)
                .createdAt(OffsetDateTime.now())
                .build();

        List<ListingGroupMember> members = List.of(
                ListingGroupMember.builder().groupId(groupId).listingId(listingId1).build(),
                ListingGroupMember.builder().groupId(groupId).listingId(listingId2).build()
        );

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroupId(groupId)).thenReturn(members);

        ListingGroupResponse response = listingGroupService.getGroup(groupId);

        assertThat(response.id()).isEqualTo(groupId);
        assertThat(response.name()).isEqualTo("Beach Villas");
        assertThat(response.listingIds()).containsExactly(listingId1, listingId2);
        verify(groupRepository).findById(groupId);
        verify(memberRepository).findByGroupId(groupId);
    }
}
