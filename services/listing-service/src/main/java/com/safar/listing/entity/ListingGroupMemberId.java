package com.safar.listing.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingGroupMemberId implements Serializable {
    private UUID groupId;
    private UUID listingId;
}
