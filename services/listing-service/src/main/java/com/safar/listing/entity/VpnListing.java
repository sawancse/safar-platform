package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vpn_listings", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "commission_pct", nullable = false)
    @Builder.Default
    private Integer commissionPct = 10;

    @Column(name = "open_to_network", nullable = false)
    @Builder.Default
    private Boolean openToNetwork = false;

    @Column(name = "min_stay_nights")
    @Builder.Default
    private Integer minStayNights = 1;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "available_to")
    private LocalDate availableTo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
