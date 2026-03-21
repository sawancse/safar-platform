package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "managed_stay_contracts", schema = "listings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ManagedStayContract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "management_fee_pct", nullable = false)
    @Builder.Default
    private Integer managementFeePct = 18;

    @Column(name = "contract_start", nullable = false)
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "auto_pricing", nullable = false)
    @Builder.Default
    private Boolean autoPricing = true;

    @Column(name = "auto_cleaning", nullable = false)
    @Builder.Default
    private Boolean autoCleaning = true;

    @Column(name = "guest_screening", nullable = false)
    @Builder.Default
    private Boolean guestScreening = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
