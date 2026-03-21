package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vpn_referrals", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "referrer_id", nullable = false)
    private UUID referrerId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "commission_paise")
    private Long commissionPaise;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;
}
