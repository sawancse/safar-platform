package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "guaranteed_income_contracts", schema = "listings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GuaranteedIncomeContract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "monthly_guarantee_paise", nullable = false)
    private Long monthlyGuaranteePaise;

    @Column(name = "contract_start", nullable = false)
    private LocalDate contractStart;

    @Column(name = "contract_end", nullable = false)
    private LocalDate contractEnd;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "total_paid_out_paise", nullable = false)
    @Builder.Default
    private Long totalPaidOutPaise = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
