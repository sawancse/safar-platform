package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Materialized stats for the donation page — updated on each captured donation.
 * Single row (singleton pattern) to avoid expensive aggregation queries.
 */
@Entity
@Table(name = "donation_stats", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Builder.Default
    private Long totalRaisedPaise = 0L;

    @Builder.Default
    private Long goalPaise = 50000000L; // ₹5,00,000 default goal

    @Builder.Default
    private Integer totalDonors = 0;

    @Builder.Default
    private Integer familiesHoused = 0;

    @Builder.Default
    private Integer monthlyDonors = 0;

    /** Current active campaign name */
    private String activeCampaign;

    /** Campaign tagline */
    private String campaignTagline;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
