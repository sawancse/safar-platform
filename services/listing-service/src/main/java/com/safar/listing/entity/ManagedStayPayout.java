package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "managed_stay_payouts", schema = "listings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ManagedStayPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "gross_revenue", nullable = false)
    private Long grossRevenue;

    @Column(nullable = false)
    @Builder.Default
    private Long expenses = 0L;

    @Column(name = "management_fee", nullable = false)
    private Long managementFee;

    @Column(name = "net_payout", nullable = false)
    private Long netPayout;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "payout_date")
    private LocalDate payoutDate;
}
