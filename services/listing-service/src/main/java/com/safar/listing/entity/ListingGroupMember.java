package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "listing_group_members", schema = "listings")
@IdClass(ListingGroupMemberId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingGroupMember {

    @Id
    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Id
    @Column(name = "listing_id", nullable = false)
    private UUID listingId;
}
